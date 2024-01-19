package no.nav.etterlatte.libs.common.klage

import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class StatistikkKlage(
    val id: UUID,
    val sak: Sak,
    val tidspunkt: Tidspunkt,
    val saksbehandler: String? = null,
)

enum class KlageHendelseType {
    OPPRETTET,
    FERDIGSTILT,
}

const val STATISTIKK_RIVER_KEY = "KLAGE"
