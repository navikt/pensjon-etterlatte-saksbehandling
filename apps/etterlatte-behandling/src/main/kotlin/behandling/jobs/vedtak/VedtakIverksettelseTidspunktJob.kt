package no.nav.etterlatte.behandling.jobs.vedtak

import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

// jobb for å sette iverksettelseTidspunkt for vedtak
class VedtakIverksettelseTidspunktJob(
    private val service: VedtakIverksettelseTidspunktMigreringService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val interval: Duration,
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
        ) {
            if (erLeader()) {
                service.run()
            }
        }
    }
}
