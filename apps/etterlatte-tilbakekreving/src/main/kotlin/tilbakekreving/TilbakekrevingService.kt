package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.tilbakekreving.klienter.TilbakekrevingskomponentenKlient

class TilbakekrevingService(
    private val tilbakekrevingskomponentenKlient: TilbakekrevingskomponentenKlient,
) {
    fun sendTilbakekrevingsvedtak(tilbakekrevingsvedtak: TilbakekrevingVedtak) =
        tilbakekrevingskomponentenKlient.sendTilbakekrevingsvedtak(tilbakekrevingsvedtak)

    fun hentKravgrunnlag(
        kravgrunnlagId: Long,
        sakId: SakId,
    ): Kravgrunnlag? = tilbakekrevingskomponentenKlient.hentKravgrunnlag(sakId, kravgrunnlagId)

    fun hentKravgrunnlagOmgjoering(
        kravgrunnlagId: Long,
        sakId: SakId,
        saksbehandler: String,
        enhet: Enhetsnummer,
    ): Kravgrunnlag? =
        tilbakekrevingskomponentenKlient.hentKravgrunnlagOmgjoering(
            sakId = sakId,
            kravgrunnlagId = kravgrunnlagId,
            saksbehandler = saksbehandler,
            enhet = enhet,
        )
}
