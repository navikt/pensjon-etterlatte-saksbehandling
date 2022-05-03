package no.nav.etterlatte.avstemming

import no.nav.etterlatte.config.LeaderElection
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Duration
import java.util.*
import kotlin.concurrent.fixedRateTimer

class GrensesnittsavstemmingJob(
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService,
    private val leaderElection: LeaderElection,
    private val starttidspunkt: Date,
    private val periode: Duration,
) {
    private val jobbNavn = this::class.simpleName

    fun planlegg() {
        fixedRateTimer(
            name = jobbNavn,
            daemon = true,
            startAt = starttidspunkt,
            period = periode.toMillis()
        ) {
            try {
                Grensesnittsavstemming(
                    grensesnittsavstemmingService = grensesnittsavstemmingService,
                    leaderElection = leaderElection,
                    jobbNavn = jobbNavn!!,
                ).run()
            } catch (throwable: Throwable) {
                logger.error("Avstemming feilet", throwable)
            }
        }
    }

    class Grensesnittsavstemming(
        val grensesnittsavstemmingService: GrensesnittsavstemmingService,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            if (leaderElection.isLeader()) {
                log.info("Starter $jobbNavn")
                // Ktor legger på X-Correlation-ID for web-requests, men vi har ikke noe tilsvarende automagi for meldingskøen.
                MDC.put(
                    "X-Correlation-ID",
                    UUID.randomUUID().toString()
                ) // TODO: bytte ut denne med vår egen x-correlation id?
                grensesnittsavstemmingService.startGrensesnittsavstemming()
            }
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(GrensesnittsavstemmingJob::class.java)
    }
}
