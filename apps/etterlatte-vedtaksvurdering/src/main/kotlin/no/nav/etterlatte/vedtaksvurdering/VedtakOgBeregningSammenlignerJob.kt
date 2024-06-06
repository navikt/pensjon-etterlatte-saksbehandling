package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.jobs.LoggerInfo
import no.nav.etterlatte.jobs.fixedRateCancellableTimer
import no.nav.etterlatte.libs.common.TimerJob
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.vedtaksvurdering.BehandlingOgSaktype
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Timer

class VedtakOgBeregningSammenlignerJob(
    private val beregningKlient: BeregningKlient,
    private val vedtaksvurderingRepository: VedtaksvurderingRepository,
    private val erLeader: () -> Boolean = { true },
    private val initialDelay: Long = Duration.of(2, ChronoUnit.MINUTES).toMillis(),
    private val periode: Duration = Duration.of(12, ChronoUnit.HOURS),
) : TimerJob {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val jobbNavn = this::class.simpleName

    override fun schedule(): Timer {
        logger.info("$jobbNavn er satt til å kjøre med periode $periode")

        return fixedRateCancellableTimer(
            name = jobbNavn,
            initialDelay = initialDelay,
            period = periode.toMillis(),
            loggerInfo = LoggerInfo(logger = logger, sikkerLogg = logger, loggTilSikkerLogg = false),
        ) {
            if (erLeader()) {
                sjekkAlle()
            }
        }
    }

    private fun sjekkAlle() {
        val alleLoependeVedtak = vedtaksvurderingRepository.hentAlleLoependeVedtak()
        alleLoependeVedtak.chunked(100).forEach { chunk ->
            chunk.forEach {
                runBlocking { start(it) }
            }
            Thread.sleep(Duration.ofSeconds(5))
        }
    }

    private suspend fun start(behandling: BehandlingOgSaktype) {
        val beregning =
            beregningKlient.hentBeregningOgAvkorting(
                behandlingId = behandling.behandlingId,
                brukerTokenInfo = Systembruker.automatiskJobb,
                saktype = behandling.sakType,
            )
        val vedtak = vedtaksvurderingRepository.hentVedtak(behandling.behandlingId)
        vedtak?.let {
            logger.debug("Sjekker perioder og beregning for vedtak ${vedtak.id} i sak ${vedtak.sakId}")
            try {
                VedtakOgBeregningSammenligner.sammenlign(beregning, it)
                logger.debug("Sjekk OK mellom perioder og beregning for vedtak ${vedtak.id} i sak ${vedtak.sakId}")
            } catch (e: IllegalStateException) {
                logger.warn(
                    "Mismatch mellom perioder for utbetaling og beregning " +
                        "for vedtak ${vedtak.id} i sak ${vedtak.sakId}",
                )
            } catch (e: Exception) {
                logger.error(
                    "Klarte ikke å sjekke match mellom perioder for utbetaling og beregning " +
                        "for vedtak ${vedtak.id} i sak ${vedtak.sakId}",
                )
            }
        }
    }
}
