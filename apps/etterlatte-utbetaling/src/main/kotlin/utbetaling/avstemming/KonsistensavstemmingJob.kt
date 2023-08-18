package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.*

class KonsistensavstemmingJob(
    private val konsistensavstemmingService: KonsistensavstemmingService,
    private val kjoereplan: Set<LocalDate>,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
    private val clock: Clock,
    private val saktype: Saktype
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            period = periode.toMillis(),
            logger = logger,
            sikkerLogg = sikkerLogg,
            loggTilSikkerLogg = true
        ) {
            Konsistensavstemming(
                konsistensavstemmingService = konsistensavstemmingService,
                kjoereplan = kjoereplan,
                leaderElection = leaderElection,
                jobbNavn = jobbNavn!!,
                clock = clock,
                saktype = saktype
            ).run()
        }
    }

    class Konsistensavstemming(
        val konsistensavstemmingService: KonsistensavstemmingService,
        val kjoereplan: Set<LocalDate>,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
        val clock: Clock,
        val saktype: Saktype
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            val idag = LocalDate.now(clock.norskKlokke())
            kjoereplan.find { dato -> dato == idag }?.let {
                if (leaderElection.isLeader()) {
                    log.info("Starter $jobbNavn")
                    if (!konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                            saktype = saktype,
                            idag = idag
                        )
                    ) {
                        konsistensavstemmingService.startKonsistensavstemming(dag = idag, saktype = saktype)
                    } else {
                        log.info("Konsistensavstemming er allerede kjoert for ${saktype.name} i dag ($idag)")
                    }
                }
            } ?: log.info("Denne datoen ($idag) er ikke en del av kjøreplanen for konsistensavstemming")
        }
    }
}