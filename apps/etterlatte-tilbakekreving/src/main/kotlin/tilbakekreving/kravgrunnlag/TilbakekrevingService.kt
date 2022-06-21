package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.time.Clock

class TilbakekrevingService(
    val tilbakekrevingDao: TilbakekrevingDao,
    val kravgrunnlagMapper: KravgrunnlagMapper,
    val clock: Clock
) {
    fun lagreKravgrunnlag(detaljertKravgrunnlag: DetaljertKravgrunnlagDto, kravgrunnlagXml: String) {
        val kravgrunnlag = kravgrunnlagMapper.toKravgrunnlag(detaljertKravgrunnlag, kravgrunnlagXml)
        val lagretKravgrunnlag = tilbakekrevingDao.lagreKravgrunnlag(kravgrunnlag, Tidspunkt.now(clock))
    }
}