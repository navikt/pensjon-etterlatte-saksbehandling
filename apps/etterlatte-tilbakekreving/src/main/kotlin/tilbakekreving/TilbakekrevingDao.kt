package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.tilbakekreving.domene.TilbakekrevingsmeldingDto
import javax.sql.DataSource

class TilbakekrevingDao(
    val dataSource: DataSource
) {
    fun lagreKravgrunnlag(kravgrunnlag: TilbakekrevingsmeldingDto) {
        TODO("Not yet implemented")
    }
}