package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.time.Clock

class TilbakekrevingService(
    val tilbakekrevingDao: TilbakekrevingDao,
    val kravgrunnlagMapper: KravgrunnlagMapper,
    val clock: Clock
) {
    fun lagreKravgrunnlag(detaljertKravgrunnlag: DetaljertKravgrunnlagDto) {
        val tidspunkt = Tidspunkt.now(clock)
        val kravgrunnlag = kravgrunnlagMapper.toKravgrunnlag(detaljertKravgrunnlag)
        tilbakekrevingDao.lagreKravgrunnlag(kravgrunnlag, tidspunkt)
    }
}