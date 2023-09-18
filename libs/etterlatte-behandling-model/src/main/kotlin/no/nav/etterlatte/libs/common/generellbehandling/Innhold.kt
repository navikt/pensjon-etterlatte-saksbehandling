package no.nav.etterlatte.libs.common.generellbehandling

import java.util.*

sealed class Innhold {
    data class Utland(
        val sed: String,
        val tilknyttetBehandling: UUID
    ) : Innhold()

    data class Annen(
        val innhold: String
    ) : Innhold()
}