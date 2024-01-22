package no.nav.etterlatte.libs.common.tilbakekreving

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class StatistikkTilbakekrevingDto(
    val id: UUID,
    val tilbakekreving: StatistikkTilbakekreving,
    val tidspunkt: Tidspunkt,
)

data class StatistikkTilbakekreving(
    val id: UUID,
    val sak: Sak,
    val behandlingOpprettet: Tidspunkt,
    val soeknadMottattDato: Tidspunkt?,
    val status: String,
    val type: String,
)

enum class TilbakekrevingHendelseType {
    OPPRETTET,
    FERDIGSTILT,
}

fun TilbakekrevingHendelseType.lagEventnameForType(): String = "TILBAKEKREVING:${this.name}"

const val TILBAKEKREVING_STATISTIKK_RIVER_KEY = "TILBAKEKREVING"
