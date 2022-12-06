package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.utbetaling.config.LeaderElection
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.fixedRateTimer

class KonsistensavstemmingJob(
    private val konsistensavstemmingService: KonsistensavstemmingService,
    private val leaderElection: LeaderElection,
    private val starttidspunkt: Date,
    private val periode: Duration
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    fun schedule() =
        fixedRateTimer(
            name = jobbNavn,
            daemon = true,
            startAt = starttidspunkt,
            period = periode.toMillis()
        ) {
            try {
                Konsistensavstemming(
                    konsistensavstemmingService = konsistensavstemmingService,
                    leaderElection = leaderElection,
                    jobbNavn = jobbNavn!!
                ).run()
            } catch (throwable: Throwable) {
                logger.error("Konsistensavstemming feilet", throwable)
            }
        }

    class Konsistensavstemming(
        val konsistensavstemmingService: KonsistensavstemmingService,
        val leaderElection: LeaderElection,
        val jobbNavn: String
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            log.info("Starter $jobbNavn")
            val idag = LocalDate.now()
            withLogContext {
                if (leaderElection.isLeader()) {
                    Saktype.values().forEach {
                        when (it) {
                            Saktype.BARNEPENSJON -> {
                                log.info("Konsistensavstemming for barnepensjon ennaa ikke implementert")
                                /* TODO: fjern kommentar under naar klar til aa kjoere avstemming*/
                                // konsistensavstemmingService.startKonsistensavstemming(idag, it)
                            }
                            Saktype.OMSTILLINGSSTOENAD -> {
                                log.info("Konsistensavstemming for omstillingsstoenad ennaa ikke implementert")
                                /* TODO: Blir haandtert i EY-1274 */
                            }
                        }
                    }
                }
            }
        }
    }
}