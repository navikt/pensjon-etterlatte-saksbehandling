package no.nav.etterlatte.tidshendelser

import net.logstash.logback.marker.Markers
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class HendelsePollerTask(
    private val initialDelaySeconds: Long,
    private val periode: Duration,
    private val hendelsePoller: HendelsePoller,
    private val maxAntallHendelsePerPoll: Int = 5,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(HendelsePollerTask::class.java)

    override fun schedule(): Timer {
        logger.info("Starter polling av nye hendelser pr $periode")

        return fixedRateCancellableTimer(
            name = "HENDELSE_POLLER_TASK",
            initialDelay = initialDelaySeconds * 1000,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger),
        ) {
            hendelsePoller.poll(maxAntallHendelsePerPoll)
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
                hendelseDao
                    .hentJobber(hendelser.map { it.jobbId }.distinct())
                    .associateBy { it.id }

            hendelser.forEach {
                val markers =
                    Markers.appendEntries(
                        mapOf(
                            "jobbId" to it.jobbId.toString(),
                            "hendelseId" to it.id.toString(),
                            "sakId" to it.sakId.toString(),
                            "type" to jobsById[it.jobbId]!!.type.toString(),
                        ),
                    )

                logger.info(markers, "Behandler hendelse: [id=${it.id}, sakId=${it.sakId}]")

                hendelseDao.oppdaterHendelseStatus(it.id, HendelseStatus.SENDT)

                try {
                    hendelsePublisher.publish(hendelse = it, jobb = jobsById[it.jobbId]!!)
                } catch (e: Exception) {
                    logger.error(markers, "Feil ved publisering av hendelse", e)
                    hendelseDao.oppdaterHendelseStatus(it.id, HendelseStatus.FEILET)
                }
            }
        }
    }
}
