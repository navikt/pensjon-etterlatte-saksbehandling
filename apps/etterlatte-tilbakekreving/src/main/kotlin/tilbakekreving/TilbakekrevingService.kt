package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.tilbakekreving.klienter.TilbakekrevingKlient
import org.slf4j.LoggerFactory

class TilbakekrevingService(
    private val tilbakekrevingKlient: TilbakekrevingKlient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendTilbakekrevingsvedtak(tilbakekrevingsvedtak: TilbakekrevingVedtak) {
        logger.info("Sender tilbakekrevingsvedtak (${tilbakekrevingsvedtak.vedtakId}) til tilbakekrevingskomponenten")
        tilbakekrevingKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)
    }

    fun hentKravgrunnlag(
        kravgrunnlagId: Long,
        sakId: Long,
    ): Kravgrunnlag {
        logger.info("Henter oppdatert kravgrunnlag for sak $sakId fra tilbakekrevingskomponenten")
        return tilbakekrevingKlient.hentKravgrunnlag(sakId, kravgrunnlagId)
    }
}
