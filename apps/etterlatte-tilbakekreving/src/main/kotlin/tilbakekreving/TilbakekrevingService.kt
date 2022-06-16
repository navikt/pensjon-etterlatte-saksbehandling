package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.tilbakekreving.domene.TilbakekrevingsmeldingDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.time.Clock

class TilbakekrevingService(
    val tilbakekrevingDao: TilbakekrevingDao,
    val clock: Clock
) {
    fun lagreKravgrunnlag(kravgrunnlag: TilbakekrevingsmeldingDto) {
        tilbakekrevingDao.lagreKravgrunnlag(kravgrunnlag)
    }
}