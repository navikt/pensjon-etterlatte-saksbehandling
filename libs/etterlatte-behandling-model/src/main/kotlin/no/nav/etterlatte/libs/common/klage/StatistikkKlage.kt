package no.nav.etterlatte.libs.common.klage

import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class StatistikkKlage(
    val id: UUID,
    val klage: Klage,
    val tidspunkt: Tidspunkt,
    val saksbehandler: String? = null,
)

enum class KlageHendelseType {
    OPPRETTET,
    FERDIGSTILT,
    KABAL_HENDELSE,
}

fun KlageHendelseType.lagEventnameForType(): String = "KLAGE:${this.name}"

const val KLAGE_STATISTIKK_RIVER_KEY = "KLAGE"
