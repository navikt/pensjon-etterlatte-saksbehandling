package no.nav.etterlatte.vedtaksvurdering.samordning

import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.concurrent.thread

class KorrigerVedtak(private val vedtaksvurderingService: VedtakService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun fiksVedtakForBehandling() {
        thread {
            Thread.sleep(60_000)
            val behandlingId = "243a591e-12e5-4f8c-a97d-956e0c61025e"
            logger.info("Kjører korrigeringsjobb for behandling $behandlingId")
            val vedtak = vedtaksvurderingService.hentVedtak(UUID.fromString(behandlingId))
            if (vedtak?.status == VedtakStatus.TIL_SAMORDNING) {
                vedtaksvurderingService.samordnetVedtak(vedtak.id.toString())
            }
        }
    }
}
