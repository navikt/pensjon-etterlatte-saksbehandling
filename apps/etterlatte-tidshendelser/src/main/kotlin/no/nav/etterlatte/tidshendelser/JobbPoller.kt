package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.jobs.LeaderElection
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.util.Date
import java.util.Timer

class JobbPollerTask(
    private val leaderElection: LeaderElection,
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val clock: Clock,
    private val jobbPoller: JobbPoller,
) {
    private val logger = LoggerFactory.getLogger(JobbPollerTask::class.java)

    fun start(): Timer {
        logger.info("Starter polling av jobber pr $periode")

        return fixedRateCancellableTimer(
            name = "JOBB_POLLER_TASK",
            startAt = Date(clock.millis() + (initialDelaySeconds * 1000)),
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
        ) {
            if (leaderElection.isLeader()) {
                jobbPoller.poll()
            }
        }
    }
}

class JobbPoller(
    private val hendelseDao: HendelseDao,
    private val aldersovergangerService: AldersovergangerService,
) {
    private val logger = LoggerFactory.getLogger(JobbPoller::class.java)

    fun poll() {
        logger.info("Sjekker for jobber å starte...")

        hendelseDao.finnAktuellJobb().forEach {
            logger.info("Fant jobb ${it.id} med type ${it.type}, status (${it.status})")

            when (it.type) {
                JobbType.AO_BP20 -> aldersovergangerService.execute(it)
                JobbType.AO_BP18 -> logger.warn("Ikke implementert: AO_BP18")
            }
        }
    }
}
