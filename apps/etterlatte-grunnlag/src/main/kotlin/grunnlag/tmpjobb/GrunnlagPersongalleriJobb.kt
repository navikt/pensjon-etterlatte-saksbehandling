package no.nav.etterlatte.grunnlag.tmpjobb

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.common.isDev
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

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
            if (erLeader() && isDev()) {
                measureTimedValue {
                    grunnlagPersongalleriService.kjoer()
                }.let { (_, varighet) ->
                    logger.info("Varighet for $jobbNavn ${varighet.toString(DurationUnit.SECONDS, 2)}")
                }
            } else {
                logger.info("Er ikke leader og kjører ikke $jobbNavn")
            }
        }
    }
}
