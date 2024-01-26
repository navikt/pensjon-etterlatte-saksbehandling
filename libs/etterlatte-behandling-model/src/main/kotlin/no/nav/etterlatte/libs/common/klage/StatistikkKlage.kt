package no.nav.etterlatte.libs.common.klage

import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.rapidsandrivers.EventnameHendelseType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class StatistikkKlage(
    val id: UUID,
    val klage: Klage,
    val tidspunkt: Tidspunkt,
    val saksbehandler: String? = null,
)

enum class KlageHendelseType : EventnameHendelseType {
    OPPRETTET,
    FERDIGSTILT,
    KABAL_HENDELSE,
    ;

    override fun lagEventnameForType(): String = "KLAGE:${this.name}"
}

const val KLAGE_STATISTIKK_RIVER_KEY = "KLAGE"
