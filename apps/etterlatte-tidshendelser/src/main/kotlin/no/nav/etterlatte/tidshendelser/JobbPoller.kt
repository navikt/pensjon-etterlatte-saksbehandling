package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.TimerJob
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.util.Timer

class JobbPollerTask(
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val klokke: Clock,
    private val openingHours: OpeningHours,
    private val jobbPoller: JobbPoller,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(JobbPollerTask::class.java)

    override fun schedule(): Timer {
        logger.info("Starter polling av jobber pr $periode")

        return fixedRateCancellableTimer(
            name = "JOBB_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
        ) {
            if (openingHours.isOpen(klokke)) {
                jobbPoller.poll()
            }
        }
    }
}

class JobbPoller(
    private val hendelseDao: HendelseDao,
    private val aldersovergangerService: AldersovergangerService,
    private val omstillingsstoenadService: OmstillingsstoenadService,
) {
    private val logger = LoggerFactory.getLogger(JobbPoller::class.java)

    fun poll() {
        logger.info("Sjekker for jobber å starte...")

        hendelseDao.finnAktuellJobb().forEach {
            logger.info("Fant jobb ${it.id}, type=${it.type}, status=${it.status}")
            hendelseDao.oppdaterJobbstatusStartet(it)

            val saker =
                when (it.type.kategori) {
                    JobbKategori.ALDERSOVERGANG -> aldersovergangerService.execute(it)
                    JobbKategori.OMS_DOEDSDATO -> omstillingsstoenadService.execute(it)
                }

            if (saker.isEmpty()) {
                // Nuttin' to do
                val jobbRefreshed = hendelseDao.hentJobb(it.id)
                hendelseDao.oppdaterJobbstatusFerdig(jobbRefreshed)
            }
        }
    }
}

data class OpeningHours(val start: Int, val slutt: Int) {
    companion object {
        fun of(openingHours: String): OpeningHours {
            openingHours.split("-").toList()
                .also { require(it.size == 2) }
                .let { return OpeningHours(it[0].toInt(), it[1].toInt()) }
        }
    }

    fun isOpen(klokke: Clock): Boolean {
        val time = klokke.instant().atZone(klokke.zone).hour
        return time in start until slutt
    }
}
