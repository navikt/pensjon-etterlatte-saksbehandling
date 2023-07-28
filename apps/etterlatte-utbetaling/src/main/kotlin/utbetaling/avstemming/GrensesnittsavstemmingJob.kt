package no.nav.etterlatte.utbetaling.grensesnittavstemming

import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class GrensesnittsavstemmingJob(
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService,
    private val leaderElection: LeaderElection,
    private val starttidspunkt: Date,
    private val periode: Duration,
    private val saktype: Saktype
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    fun schedule(): Timer {
        log.info("$jobbNavn er satt til å starte $starttidspunkt med periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            startAt = starttidspunkt,
            period = periode.toMillis(),
            logger = log,
            sikkerLogg = sikkerLogg
        ) {
            Grensesnittsavstemming(
                grensesnittsavstemmingService = grensesnittsavstemmingService,
                leaderElection = leaderElection,
                jobbNavn = jobbNavn!!,
                saktype = saktype
            ).run()
        }
    }

    class Grensesnittsavstemming(
        val grensesnittsavstemmingService: GrensesnittsavstemmingService,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
        val saktype: Saktype
    ) {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun run() {
            log.info("Starter $jobbNavn")
            if (leaderElection.isLeader()) {
                grensesnittsavstemmingService.startGrensesnittsavstemming(saktype)
            }
        }
    }
}