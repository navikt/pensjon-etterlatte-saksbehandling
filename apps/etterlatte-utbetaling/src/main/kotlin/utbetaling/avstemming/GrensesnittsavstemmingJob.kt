package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.concurrent.fixedRateTimer

class GrensesnittsavstemmingJob(
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService,
    private val leaderElection: LeaderElection,
    private val starttidspunkt: Date,
    private val periode: Duration,
    private val omstillingstonadEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    fun schedule(): Timer {
        log.info("$jobbNavn er satt til å starte $starttidspunkt med periode $periode")

        return fixedRateTimer(
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
                    omstillingstonadEnabled = omstillingstonadEnabled
                ).run()
            } catch (throwable: Throwable) {
                log.error("Grensesnittavstemming feilet, se sikker logg for stacktrace")
                sikkerLogg.error("Grensesnittavstemming feilet", throwable)
            }
        }
    }

    class Grensesnittsavstemming(
        val grensesnittsavstemmingService: GrensesnittsavstemmingService,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
        private val omstillingstonadEnabled: Boolean
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            log.info("Starter $jobbNavn")
            withLogContext {
                if (leaderElection.isLeader()) {
                    Saktype.values().forEach {
                        //Dette kan nok forenkles
                        when (it) {
                            Saktype.BARNEPENSJON -> {
                                grensesnittsavstemmingService.startGrensesnittsavstemming(it)
                            }
                            Saktype.OMSTILLINGSSTOENAD -> {
                                if(omstillingstonadEnabled) grensesnittsavstemmingService.startGrensesnittsavstemming(it)
                            }
                        }
                    }
                }
            }
        }
    }
}