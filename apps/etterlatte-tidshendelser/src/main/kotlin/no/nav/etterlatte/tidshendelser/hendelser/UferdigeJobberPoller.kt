package no.nav.etterlatte.tidshendelser.hendelser

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Date
import java.util.Timer

/**
 * Finner og error-logger jobber som ikke har status FERDIG
 * og ikke har noen jobb opprettet etterpå som er enten FERDIG, eller ikke FEILET og har kjøredato på eller etter i dag.
 * Ser ikke på jobber som har kjøredato eldre enn 3 måneder.
 */
class UferdigeJobberPollerTask(
    private val periode: Duration,
    private val startAt: Date,
    private val poller: UferdigeJobberPoller,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun schedule(): Timer {
        logger.info("Starter polling av uferdige jobber")

        return fixedRateCancellableTimer(
            name = "HENDELSE_POLLER_TASK",
            period = periode.toMillis(),
            startAt = startAt,
            loggerInfo = LoggerInfo(logger = logger),
        ) {
            poller.poll()
        }
    }
}

class UferdigeJobberPoller(
    private val hendelseDao: HendelseDao,
) {
    private val logger = LoggerFactory.getLogger(UferdigeJobberPoller::class.java)

    fun poll() {
        logger.info("Poller etter uferdige jobber")
        val jobber = hendelseDao.uferdigeJobber()

        if (jobber.isEmpty()) {
            logger.info("Fant ingen uferdige jobber")
        } else {
            loggUferdigeJobber(jobber)
        }
    }

    fun loggUferdigeJobber(jobber: List<HendelserJobb>) {
        logger.error(
            "Fant uferdige jobber som ikke vil bli forsøkt på nytt: " +
                jobber
                    .map { "(type: ${it.type}, id: ${it.id})" }
                    .joinToString { "," } +
                ". Disse må det opprettes nye jobber for.",
        )
    }
}
