package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattetVedtakDto
import org.slf4j.LoggerFactory

class VedtakTilbakekrevingService(
    private val repository: VedtakTilbakekrevingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakTilbakekrevingService::class.java)

    fun lagreVedtak(dto: TilbakekrevingFattetVedtakDto): Long {
        return repository.lagreFattetVedtak(dto).id
    }
}
