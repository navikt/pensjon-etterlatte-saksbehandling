package no.nav.etterlatte.vedtaksvurdering.resend

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRapidService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer

class ResendTilUtbetalingJob(
    private val repository: ResendTilUtbetalingRepository,
    private val vedtakBehandlingService: VedtakBehandlingService,
    private val rapidService: VedtaksvurderingRapidService,
    private val erLeader: () -> Boolean,
    private val initialDelay: Long,
    private val periode: Duration,
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun schedule(): Timer {
        logger.info("${this::class.simpleName} er satt opp med periode $periode")

        return fixedRateCancellableTimer(
            name = this::class.simpleName,
            initialDelay = initialDelay,
            loggerInfo = LoggerInfo(logger = logger, loggTilSikkerLogg = false),
            period = periode.toMillis(),
        ) {
            if (erLeader()) {
                run()
            }
        }
    }

    internal fun run() {
        val uprosesserte = repository.hentUprosesserte()
        if (uprosesserte.isEmpty()) return

        logger.info("Fant ${uprosesserte.size} vedtak som skal sendes på nytt til utbetaling")

        uprosesserte.forEach { behandlingId ->
            try {
                val vedtakOgRapid =
                    runBlocking {
                        vedtakBehandlingService.resendVedtakTilUtbetaling(behandlingId, HardkodaSystembruker.river)
                    }
                rapidService.sendToRapid(vedtakOgRapid)
                repository.merkSomProsessert(behandlingId)
                logger.info("Vedtak for behandlingId=$behandlingId er resendt til utbetaling")
            } catch (e: Exception) {
                logger.error("Feil ved resending av vedtak for behandlingId=$behandlingId", e)
            }
        }
    }
}
