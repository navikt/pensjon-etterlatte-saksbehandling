package no.nav.etterlatte.tilbakekreving.vedtak

import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import org.slf4j.LoggerFactory

class TilbakekrevingVedtakService(
    private val tilbakekrevingKlient: TilbakekrevingKlient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendTilbakekrevingsvedtak(tilbakekrevingsvedtak: TilbakekrevingVedtak) {
        logger.info("Sender tilbakekrevingsvedtak (${tilbakekrevingsvedtak.vedtakId}) til tilbakekrevingskomponenten")
        tilbakekrevingKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
    }
}
