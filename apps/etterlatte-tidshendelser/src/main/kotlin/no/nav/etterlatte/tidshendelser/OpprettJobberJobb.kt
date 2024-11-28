package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.tidshendelser.JobbType
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.YearMonth
import java.util.Timer

class OpprettJobberJobb(
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val openingHours: OpeningHours,
    private val opprettJobb: OpprettJobb,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun schedule(): Timer {
        logger.info("Starter sjekk av om jobber er lagt inn for neste måned med interval $periode")

        return fixedRateCancellableTimer(
            name = "JOBB_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
            openingHours = openingHours,
        ) {
            opprettJobb.poll()
        }
    }
}

class OpprettJobb(
    private val hendelseDao: HendelseDao,
) {
    private val logger = LoggerFactory.getLogger(JobbPoller::class.java)

    fun poll() {
        val nesteMaaned = YearMonth.now().plusMonths(1)
        logger.info("Sjekker for jobber å legge til for måned: $nesteMaaned")
        val kjoeringerNesteMaaned = hendelseDao.finnJobberMedKjoeringForMaaned(nesteMaaned)

        fjernDuplikateKjoeringerFraFasteJobber(kjoeringerNesteMaaned).forEach { fasteJobber ->
            hendelseDao.opprettJobb(fasteJobber, nesteMaaned)
        }
    }

    private fun fjernDuplikateKjoeringerFraFasteJobber(kjoreringNesteMaaned: List<HendelserJobb>) =
        FasteJobber.entries.filter { fastJobb ->
            kjoreringNesteMaaned.none { kjoringNesteMaaned ->
                kjoringNesteMaaned.type ==
                    fastJobb.jobbType
            }
        }

    enum class FasteJobber(
        val jobbType: JobbType,
        val dagIMaaned: Int,
        val behandlingMaanedJustering: Long,
    ) {
        OMS_DOED_4MND(JobbType.OMS_DOED_4MND, 1, 0),
        OMS_DOED_6MND(JobbType.OMS_DOED_6MND, 1, 0),
        OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK(JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK, 8, 0),
        OMS_DOED_12MND(JobbType.OMS_DOED_12MND, 1, 0),
    }
}
