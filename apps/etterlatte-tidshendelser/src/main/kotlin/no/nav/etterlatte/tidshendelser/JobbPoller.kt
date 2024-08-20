package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.tidshendelser.regulering.ReguleringService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class JobbPollerTask(
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val openingHours: OpeningHours,
    private val jobbPoller: JobbPoller,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun schedule(): Timer {
        logger.info("Starter polling av jobber pr $periode")

        return fixedRateCancellableTimer(
            name = "JOBB_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
            openingHours = openingHours,
        ) {
            jobbPoller.poll()
        }
    }
}

class JobbPoller(
    private val hendelseDao: HendelseDao,
    private val aldersovergangerService: AldersovergangerService,
    private val omstillingsstoenadService: OmstillingsstoenadService,
    private val reguleringService: ReguleringService,
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
                    JobbKategori.REGULERING -> reguleringService.execute(it)
                }

            if (saker.isEmpty()) {
                // Nuttin' to do
                val jobbRefreshed = hendelseDao.hentJobb(it.id)
                hendelseDao.oppdaterJobbstatusFerdig(jobbRefreshed)
            }
        }
    }
}
