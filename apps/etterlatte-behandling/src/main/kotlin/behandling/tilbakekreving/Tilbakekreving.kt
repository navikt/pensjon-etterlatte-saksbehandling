package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import java.util.UUID

data class Tilbakekreving(
    val id: UUID,
    val status: TilbakekrevingStatus,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val kravgrunnlag: Kravgrunnlag,
) {
    companion object {
        fun ny(
            kravgrunnlag: Kravgrunnlag,
            sak: Sak,
        ) = Tilbakekreving(
            id = UUID.randomUUID(),
            status = TilbakekrevingStatus.OPPRETTET,
            sak = sak,
            opprettet = Tidspunkt.now(),
            kravgrunnlag = kravgrunnlag,
        )
    }
}

enum class TilbakekrevingStatus {
    OPPRETTET,
    UNDER_ARBEID,
}
