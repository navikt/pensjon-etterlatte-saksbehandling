package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.YearMonth
import java.util.Timer

class JobbSchedulerTask(
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val openingHours: OpeningHours,
    private val jobbScheduler: JobbScheduler,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun schedule(): Timer {
        logger.info("Starter sjekk av at alle periodiske jobber er planlagt for neste måned / dag, med intervall $periode")

        return fixedRateCancellableTimer(
            name = "JOBB_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
            openingHours = openingHours,
        ) {
            jobbScheduler.scheduleMaanedligeJobber()
        }
    }
}

class JobbScheduler(
    private val hendelseDao: HendelseDao,
) {
    private val logger = LoggerFactory.getLogger(JobbPoller::class.java)

    fun scheduleMaanedligeJobber() {
        val nesteMaaned = YearMonth.now().plusMonths(1)
        logger.info("Sjekker for jobber å legge til for måned: $nesteMaaned")

        val planlagteJobberNesteMnd = hendelseDao.finnJobberMedKjoeringForMaaned(nesteMaaned)

        PeriodiskeMaanedligeJobber.entries
            // filtrere bort jobber som allerede er planlagt for neste måned
            .filter { periodiskJobb ->
                planlagteJobberNesteMnd.none { kjoering -> kjoering.type == periodiskJobb.jobbType }
            }
            // opprett jobb for neste måned
            .forEach { periodiskJobb ->
                hendelseDao.opprettMaanedligJobb(periodiskJobb, nesteMaaned)
            }
    }

    enum class PeriodiskeMaanedligeJobber(
        val jobbType: JobbType,
        val dagIMaaned: Int,
        // Merk at justering av behandlingMaaned kan medføre uønsket oppførsel (f.eks. har løpende ytelse sjekker feil måned)
        val behandlingMaanedJustering: Long,
    ) {
        OMS_DOED_4MND(JobbType.OMS_DOED_4MND, 1, 0),
        OMS_DOED_6MND(JobbType.OMS_DOED_6MND, 1, 0),
        OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK(JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK, 8, 0),
        OMS_DOED_12MND(JobbType.OMS_DOED_12MND, 1, 0),
        OMS_DOED_10MND(JobbType.OMS_DOED_10MND, 1, 0),
    }
}
