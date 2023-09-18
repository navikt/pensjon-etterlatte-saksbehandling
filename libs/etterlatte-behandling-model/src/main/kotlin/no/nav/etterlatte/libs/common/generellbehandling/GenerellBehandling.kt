package no.nav.etterlatte.libs.common.generellbehandling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class GenerellBehandling(
    val id: UUID,
    val sakId: Long,
    val opprettet: Tidspunkt,
    val type: GenerellBehandlingType,
    val innhold: Innhold? = null
) {
    companion object {
        fun opprettFraType(
            type: GenerellBehandlingType,
            sakId: Long
        ) = GenerellBehandling(UUID.randomUUID(), sakId, Tidspunkt.now(), type)
    }

    enum class GenerellBehandlingType {
        ANNEN,
        UTLAND
    }
}