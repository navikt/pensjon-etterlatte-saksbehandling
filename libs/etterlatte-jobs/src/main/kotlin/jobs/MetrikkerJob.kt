package no.nav.etterlatte.jobs

import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

interface MetrikkUthenter {
    fun run()
}

class MetrikkerJob(
    private val uthenter: MetrikkUthenter,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val periode: Duration,
    private val openingHours: OpeningHours,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med uthenter=${uthenter::class.simpleName} og periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
            openingHours = openingHours,
        ) {
            if (erLeader()) {
                uthenter.run()
            }
        }
    }
}
