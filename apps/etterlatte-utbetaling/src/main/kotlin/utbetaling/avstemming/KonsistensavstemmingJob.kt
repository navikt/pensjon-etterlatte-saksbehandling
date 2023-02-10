package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.fixedRateTimer

class KonsistensavstemmingJob(
    private val konsistensavstemmingService: KonsistensavstemmingService,
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
                Konsistensavstemming(
                    konsistensavstemmingService = konsistensavstemmingService,
                    leaderElection = leaderElection,
                    jobbNavn = jobbNavn!!
                ).run()
            } catch (throwable: Throwable) {
                logger.error("Konsistensavstemming feilet", throwable)
            }
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
                                if (!konsistensavstemmingService.konsistensavstemmingErKjoertIDag(
                                        saktype = it,
                                        idag = idag
                                    )
                                ) {
                                    konsistensavstemmingService.startKonsistensavstemming(dag = idag, saktype = it)
                                } else {
                                    log.info("Konsistensavstemming er allerede kjoert i dag")
                                }
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