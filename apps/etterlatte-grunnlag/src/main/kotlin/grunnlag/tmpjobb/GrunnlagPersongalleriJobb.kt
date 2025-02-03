package no.nav.etterlatte.grunnlag.tmpjobb

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class GrunnlagPersongalleriJobb(
    private val grunnlagPersongalleriService: GrunnlagPersongalleriService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med ${grunnlagPersongalleriService::class.simpleName} og periode $interval")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = interval.toMillis(),
        ) {
            if (erLeader()) {
                grunnlagPersongalleriService.kjoer()
            }
        }
    }
}
