package no.nav.etterlatte.jobs

import no.nav.etterlatte.libs.jobs.LeaderElection
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

interface MetrikkUthenter {
    fun run()
}

class MetrikkerJob(
    private val uthenter: MetrikkUthenter,
    private val leaderElection: LeaderElection,
    private val initialDelay: Long,
    private val periode: Duration,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med uthenter=${uthenter::class.simpleName} og periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
        ) {
            if (leaderElection.isLeader()) {
                uthenter.run()
            }
        }
    }
}
