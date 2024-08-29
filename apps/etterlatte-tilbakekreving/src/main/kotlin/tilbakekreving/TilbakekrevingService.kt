package no.nav.etterlatte.tilbakekreving

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
    ): Kravgrunnlag = tilbakekrevingskomponentenKlient.hentKravgrunnlag(sakId, kravgrunnlagId)
}
