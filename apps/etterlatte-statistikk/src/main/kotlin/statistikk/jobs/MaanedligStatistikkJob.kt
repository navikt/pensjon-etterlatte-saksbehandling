package no.nav.etterlatte.statistikk.jobs

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.tidspunkt.klokke
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.statistikk.database.KjoertStatus
import no.nav.etterlatte.statistikk.service.StatistikkService
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.YearMonth
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MaanedligStatistikkJob(
    private val statistikkService: StatistikkService,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $periode")

        return fixedRateTimer(
            name = jobbNavn,
            daemon = true,
            initialDelay = initialDelay,
            period = periode.toMillis()
        ) {
            try {
                ProduserOgLagreMaanedligStatistikk(
                    leaderElection = leaderElection,
                    statistikkService = statistikkService,
                    clock = klokke().norskKlokke()
                ).run()
            } catch (e: Throwable) {
                logger.error("Kunne ikke kjøre jobb for produsering av månedlig stønadsstatistikk på grunn av feil", e)
                throw e
            }
        }
    }

    class ProduserOgLagreMaanedligStatistikk(
        private val leaderElection: LeaderElection,
        private val statistikkService: StatistikkService,
        private val clock: Clock
    ) {
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun run() {
            val maaned = YearMonth.now(clock).minusMonths(1)
            withLogContext {
                if (!leaderElection.isLeader()) {
                    return@withLogContext
                }
                when (statistikkService.statistikkProdusertForMaaned(maaned)) {
                    KjoertStatus.IKKE_KJOERT -> {
                        val maanedsstatistikkk = statistikkService.produserStoenadStatistikkForMaaned(maaned)
                        statistikkService.lagreMaanedsstatistikk(maanedsstatistikkk)
                    }
                    KjoertStatus.FEIL -> {
                        // TODO: cleanup her og prøv på nytt -- men den trenger litt manuell håndtering for avlevering
                        //      til bigquery. EY-1821
                        logger.error("Har en kjøring med feil. Lagrer ikke ny statistikk for måned $maaned")
                    }
                    KjoertStatus.INGEN_FEIL -> logger.info("Statistikk er allerede produsert for måned $maaned")
                }
            }
        }
    }
}