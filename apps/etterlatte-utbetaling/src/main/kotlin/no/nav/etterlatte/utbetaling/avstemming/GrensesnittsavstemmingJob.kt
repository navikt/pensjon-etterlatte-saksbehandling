package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.logging.sikkerlogger
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

        /**
         * Periode for startGrensesnittsavstemming kan spesifiseres dersom man ønsker å gjøre avstemming for en gitt dato.
         * Dette kan være kjekt dersom avstemming feiler eller man av andre grunner har behov
         * for å kjøre avstemming på nytt.
         *
         * Eksempel:
         *
         * periode =
         *   Avstemmingsperiode(
         *     fraOgMed = Tidspunkt.parse("2024-01-02T23:00:00.00Z"),
         *     til = Tidspunkt.parse("2024-01-03T23:00:00.00Z"),
         *   )
         */

        fun run() {
            log.info("Starter $jobbNavn")
            if (leaderElection.isLeader()) {
                try {
                    grensesnittsavstemmingService.startGrensesnittsavstemming(saktype)
                } catch (e: UnsupportedOperationException) {
                    log.error("Feil av type ${e::class.simpleName} under kjøring i grensesnittavstemming", e)

                    // Tester ulike variasjoner av logging til team logs, vil noen av disse vises?
                    sikkerLogg.info("Test av sikkerlogg med info: Feil under kjøring i grensesnittavstemming", e)
                    sikkerLogg.error("Test av sikkerlogg med error og oe:  Feil under kjoering i grensesnittavstemming", e)
                    throw e
                } catch (e: Exception) {
                    log.error("Feil av type ${e::class.simpleName} under kjøring i grensesnittavstemming, se sikkerlogg")
                    sikkerLogg.error("Feil under kjøring i grensesnittavstemming", e)
                    throw e
                }
            }
        }
    }
}
