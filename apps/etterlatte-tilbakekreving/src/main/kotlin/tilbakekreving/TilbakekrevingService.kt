package no.nav.etterlatte.tilbakekreving.tilbakekreving

import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.time.Clock

class TilbakekrevingService(
    val tilbakekrevingDao: TilbakekrevingDao,
    val clock: Clock
) {
    fun lagreKravgrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        tilbakekrevingDao.lagreKravgrunnlag(kravgrunnlag)
    }
}