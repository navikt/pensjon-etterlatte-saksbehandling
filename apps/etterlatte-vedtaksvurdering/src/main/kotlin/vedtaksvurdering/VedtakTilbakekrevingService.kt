package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattetVedtakDto
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakTilbakekrevingService(
    private val repository: VedtakTilbakekrevingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakTilbakekrevingService::class.java)

    fun lagreVedtak(dto: TilbakekrevingFattetVedtakDto): Long {
        logger.info("Fatter vedtak for tilbakekreving=$dto.tilbakekrevingId")
        return repository.lagreFattetVedtak(dto).id
    }

    fun attesterVedtak(dto: TilbakekrevingAttesterVedtakDto): Long {
        logger.info("Attesterer vedtak for tilbakekreving=$dto.tilbakekrevingId")
        return repository.lagreAttestertVedtak(dto).id
    }

    fun underkjennVedtak(tilbakekrevingId: UUID): Long {
        logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
        return repository.lagreUnderkjentVedtak(tilbakekrevingId).id
    }
}
