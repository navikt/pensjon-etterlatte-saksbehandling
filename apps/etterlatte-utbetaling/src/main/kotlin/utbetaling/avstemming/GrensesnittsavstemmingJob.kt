package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.utbetaling.config.LeaderElection
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.concurrent.fixedRateTimer

class GrensesnittsavstemmingJob(
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService,
    private val leaderElection: LeaderElection,
    private val starttidspunkt: Date,
    private val periode: Duration
) {
    private val jobbNavn = this::class.simpleName

    fun schedule() =
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
                    jobbNavn = jobbNavn!!
                ).run()
            } catch (throwable: Throwable) {
                logger.error("Grensesnittavstemming feilet", throwable)
            }
        }

    class Grensesnittsavstemming(
        val grensesnittsavstemmingService: GrensesnittsavstemmingService,
        val leaderElection: LeaderElection,
        val jobbNavn: String
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            withLogContext {
                if (leaderElection.isLeader()) {
                    Saktype.values().forEach {
                        when (it) {
                            Saktype.BARNEPENSJON -> {
                                grensesnittsavstemmingService.startGrensesnittsavstemming(it)
                            }
                            Saktype.OMSTILLINGSSTOENAD -> {
                                log.info("Grensesnittavstemming for omstillingsoeknad er ennaa ikke implementert")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GrensesnittsavstemmingJob::class.java)
    }
}