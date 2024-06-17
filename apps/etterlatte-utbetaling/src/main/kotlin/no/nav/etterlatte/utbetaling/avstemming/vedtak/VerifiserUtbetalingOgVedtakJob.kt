package no.nav.etterlatte.utbetaling.avstemming.vedtak

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class VerifiserUtbetalingOgVedtakJob(
    private val verifiserer: Vedtaksverifiserer,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $periode")
        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, sikkerLogg = sikkerLogg, loggTilSikkerLogg = true),
        ) {
            if (leaderElection.isLeader()) {
                runBlocking {
                    verifiserer.verifiserAlle()
                }
            }
        }
    }
}
