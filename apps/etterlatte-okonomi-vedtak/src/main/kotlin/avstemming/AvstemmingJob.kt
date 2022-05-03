package no.nav.etterlatte.avstemming

import no.nav.etterlatte.config.LeaderElection
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.concurrent.fixedRateTimer

class AvstemmingJob(
    private val avstemmingService: AvstemmingService,
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
                if (leaderElection.isLeader()) {
                    avstemmingService.startAvstemming()
                }
            } catch (throwable: Throwable) {
                logger.error("Avstemming feilet", throwable)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AvstemmingJob::class.java)
    }
}
