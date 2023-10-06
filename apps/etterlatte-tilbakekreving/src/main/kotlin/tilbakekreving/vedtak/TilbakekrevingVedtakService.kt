package no.nav.etterlatte.tilbakekreving.vedtak

import org.slf4j.LoggerFactory

class TilbakekrevingVedtakService(private val tilbakekrevingKlient: TilbakekrevingKlient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendTilbakekrevingsvedtak(tilbakekrevingsvedtak: TilbakekrevingVedtak) {
        logger.info("Sender tilbakekrevingsvedtak (${tilbakekrevingsvedtak.vedtakId}) til tilbakekrevingskomponenten")
        tilbakekrevingKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
    }
}
