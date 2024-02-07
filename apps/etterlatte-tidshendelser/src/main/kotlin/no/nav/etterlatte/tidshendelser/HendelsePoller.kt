package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.jobs.LeaderElection
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class HendelsePollerTask(
    private val leaderElection: LeaderElection,
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val hendelsePoller: HendelsePoller,
    private val maxAntallHendelsePerPoll: Int = 5,
) {
    private val logger = LoggerFactory.getLogger(HendelsePollerTask::class.java)

    fun start(): Timer {
        logger.info("Starter polling av nye hendelser pr $periode")

        return fixedRateCancellableTimer(
            name = "HENDELSE_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
        ) {
            if (leaderElection.isLeader()) {
                hendelsePoller.poll(maxAntallHendelsePerPoll)
            }
        }
    }
}

class HendelsePoller(
    private val hendelseDao: HendelseDao,
    private val hendelsePublisher: HendelsePublisher,
) {
    private val logger = LoggerFactory.getLogger(HendelsePoller::class.java)

    fun poll(limit: Int) {
        logger.info("Poller etter hendelser å behandle [limit=$limit]")
        val hendelser = hendelseDao.pollHendelser(limit)

        if (hendelser.isEmpty()) {
            logger.info("Fant ingen hendelser å behandle")
        } else {
            val jobsById =
                hendelseDao.hentJobber(hendelser.map { it.jobbId }.distinct())
                    .associateBy { it.id }

            hendelser.forEach {
                withLogContext(
                    correlationId = getCorrelationId(),
                    mapOf(
                        "jobbId" to it.jobbId.toString(),
                        "hendelseId" to it.id.toString(),
                        "sakId" to it.sakId.toString(),
                        "type" to jobsById[it.jobbId]!!.type.toString(),
                    ),
                ) {
                    logger.info("Behandler hendelse: [id=${it.id}, sakId=${it.sakId}]")

                    hendelsePublisher.publish(hendelse = it, jobb = jobsById[it.jobbId]!!)

                    hendelseDao.oppdaterHendelseStatus(it, HendelseStatus.SENDT)
                }
            }
        }
    }
}
