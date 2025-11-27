package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Timer

class JobbSchedulerTask(
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val openingHours: OpeningHours,
    private val jobbScheduler: JobbScheduler,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun schedule(): Timer {
        logger.info("Starter sjekk av at alle periodiske jobber er planlagt for neste måned, med intervall $periode")

        return fixedRateCancellableTimer(
            name = "JOBB_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
            openingHours = openingHours,
        ) {
            jobbScheduler.scheduleMaanedligeJobber()

            jobbScheduler.scheduleUkentligeJobber()
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

    fun scheduleUkentligeJobber() {
        val mandagNesteUke =
            LocalDate
                .now()
                .plusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        logger.info("Sjekker for jobber å legge til for neste uke: $mandagNesteUke")

        val planlagteJobberNesteUke = hendelseDao.finnJobberMedKjoeringForUke(mandagNesteUke)

        PeriodiskeUkentligeJobber.entries
            // filtrere bort jobber som allerede er planlagt for neste uke
            .filter { periodiskJobb ->
                planlagteJobberNesteUke.none { kjoering -> kjoering.type == periodiskJobb.jobbType }
            }
            // opprett jobb for neste uke
            .forEach { periodiskJobb ->
                hendelseDao.opprettUkentligJobb(periodiskJobb, mandagNesteUke)
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
        AO_BP21(JobbType.AO_BP21, 20, 1),
        AO_BP20(JobbType.AO_BP20, 20, 1),
        AO_OMS67(JobbType.AO_OMS67, 20, 1),
        OP_BP_FYLT_18(JobbType.OP_BP_FYLT_18, 21, 2),
    }

    enum class PeriodiskeUkentligeJobber(
        val jobbType: JobbType,
        // Merk at justering av behandlingMaaned kan medføre uønsket oppførsel (f.eks. har løpende ytelse sjekker feil måned)
        val behandlingMaanedJustering: Long,
    ) {
        OPPDATER_SKJERMING_BP(JobbType.OPPDATER_SKJERMING_BP, 0),
    }
}
