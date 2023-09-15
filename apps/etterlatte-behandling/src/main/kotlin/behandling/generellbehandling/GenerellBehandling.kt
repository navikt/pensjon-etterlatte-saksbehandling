package no.nav.etterlatte.behandling.generellbehandling

import java.util.*

enum class GenerellBehandlingType {
    UTLAND,
    ANNEN
}

data class GenerellBehandling(
    val id: UUID,
    val sakId: Long,
    val type: GenerellBehandlingType,
    val innhold: Innhold
)