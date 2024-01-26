package no.nav.etterlatte.libs.common.tilbakekreving

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class StatistikkTilbakekrevingDto(
    val id: UUID,
    val tilbakekreving: TilbakekrevingBehandling,
    val tidspunkt: Tidspunkt,
)

enum class TilbakekrevingHendelseType {
    OPPRETTET,
    ATTESTERT,
}

fun TilbakekrevingHendelseType.lagEventnameForType(): String = "TILBAKEKREVING:${this.name}"

const val TILBAKEKREVING_STATISTIKK_RIVER_KEY = "TILBAKEKREVING"
