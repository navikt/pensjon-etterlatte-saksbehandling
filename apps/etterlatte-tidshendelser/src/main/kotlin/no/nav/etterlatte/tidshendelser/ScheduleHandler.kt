package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.jobs.LeaderElection
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.util.Date
import java.util.Timer

class ScheduleHandler(
    private val leaderElection: LeaderElection,
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(ScheduleHandler::class.java)

    fun start(): Timer {
        logger.info("Scheduling sjekk og eksekvering av jobber pr $periode")

        return fixedRateCancellableTimer(
            name = "SCHEDULE_HANDLER",
            startAt = Date(clock.millis() + (initialDelaySeconds * 1000)),
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, sikkerLogg = sikkerlogger(), loggTilSikkerLogg = true),
        ) {
            if (leaderElection.isLeader()) {
                JobbRunner().run()
            }
        }
    }
}
