package no.nav.etterlatte.libs.common.tilbakekreving

import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import java.util.UUID

data class StatistikkTilbakekrevingDto(
    val id: UUID,
    val tilbakekreving: TilbakekrevingBehandling,
    val tidspunkt: Tidspunkt,
    val utlandstilknytningType: UtlandstilknytningType?,
)

enum class TilbakekrevingHendelseType : EventnameHendelseType {
    OPPRETTET,
    FATTET_VEDTAK,
    ATTESTERT,
    UNDERKJENT,
    AVBRUTT,
    ;

    override fun lagEventnameForType(): String = "TILBAKEKREVING:${this.name}"
}

const val TILBAKEKREVING_STATISTIKK_RIVER_KEY = "TILBAKEKREVING"
