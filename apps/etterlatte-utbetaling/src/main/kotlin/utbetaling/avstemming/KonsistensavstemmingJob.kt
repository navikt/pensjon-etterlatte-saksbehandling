package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.fixedRateTimer

class KonsistensavstemmingJob(
    private val konsistensavstemmingService: KonsistensavstemmingService,
    private val kjoereplan: Set<LocalDate>,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
    private val clock: Clock
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
                    kjoereplan = kjoereplan,
                    leaderElection = leaderElection,
                    jobbNavn = jobbNavn!!,
                    clock = clock
                ).run()
            } catch (throwable: Throwable) {
                logger.error("Konsistensavstemming feilet", throwable)
            }
        }
    }

    class Konsistensavstemming(
        val konsistensavstemmingService: KonsistensavstemmingService,
        val kjoereplan: Set<LocalDate>,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
        val clock: Clock
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            withLogContext {
                val idag = LocalDate.now(clock.norskKlokke())
                kjoereplan.find { dato -> dato == idag }?.let {
                    if (leaderElection.isLeader()) {
                        log.info("Starter $jobbNavn")

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
                                        log.info("Konsistensavstemming er allerede kjoert i dag ($idag)")
                                    }
                                }
                                Saktype.OMSTILLINGSSTOENAD -> {
                                    log.info("Konsistensavstemming for omstillingsstoenad ennaa ikke implementert")
                                    /* TODO: Blir haandtert i EY-1274 */
                                }
                            }
                        }
                    }
                } ?: log.info("Denne datoen ($idag) er ikke en del av kjøreplanen for konsistensavstemming")
            }
        }
    }
}