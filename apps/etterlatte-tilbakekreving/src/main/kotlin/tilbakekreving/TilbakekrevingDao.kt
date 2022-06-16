package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag
import javax.sql.DataSource

class TilbakekrevingDao(
    val dataSource: DataSource
) {
    fun lagreKravgrunnlag(kravgrunnlag: Kravgrunnlag, tidspunkt: Tidspunkt) {
        TODO("Not yet implemented")
    }
}