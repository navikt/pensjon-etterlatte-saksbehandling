package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Date
import java.util.Timer

class GrensesnittsavstemmingJob(
    private val grensesnittsavstemmingService: GrensesnittsavstemmingService,
    private val leaderElection: LeaderElection,
    private val starttidspunkt: Date,
    private val periode: Duration,
    private val saktype: Saktype,
) : TimerJob {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        log.info("$jobbNavn er satt til å starte $starttidspunkt med periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            startAt = starttidspunkt,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = log, sikkerLogg = sikkerLogg, loggTilSikkerLogg = true),
        ) {
            Grensesnittsavstemming(
                grensesnittsavstemmingService = grensesnittsavstemmingService,
                leaderElection = leaderElection,
                jobbNavn = jobbNavn!!,
                saktype = saktype,
            ).run()
        }
    }

    class Grensesnittsavstemming(
        val grensesnittsavstemmingService: GrensesnittsavstemmingService,
        val leaderElection: LeaderElection,
        val jobbNavn: String,
        val saktype: Saktype,
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
