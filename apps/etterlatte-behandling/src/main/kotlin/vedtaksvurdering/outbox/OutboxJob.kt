package no.nav.etterlatte.vedtaksvurdering.outbox

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class OutboxJob(
    private val outboxService: OutboxService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med service=${outboxService::class.simpleName} og periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
        ) {
            if (erLeader()) {
                outboxService.run()
            }
        }
    }
}
