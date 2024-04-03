package no.nav.etterlatte.behandling.jobs

import no.nav.etterlatte.behandling.job.SaksbehandlerJobService
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.OpeningHours
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class SaksbehandlerJob(
    private val saksbehandlerJobService: SaksbehandlerJobService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
    private val openingHours: OpeningHours,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
            openingHours = openingHours,
        ) {
            if (erLeader()) {
                saksbehandlerJobService.run()
            }
        }
    }
}
