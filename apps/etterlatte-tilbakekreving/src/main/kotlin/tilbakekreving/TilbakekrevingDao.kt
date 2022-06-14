package no.nav.etterlatte.tilbakekreving

import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import javax.sql.DataSource

class TilbakekrevingDao(
    val dataSource: DataSource
) {
    fun lagreKravgrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        TODO("Not yet implemented")
    }
}