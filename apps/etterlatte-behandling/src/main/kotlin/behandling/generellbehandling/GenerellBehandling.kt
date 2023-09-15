package no.nav.etterlatte.behandling.generellbehandling

import java.util.*

data class GenerellBehandling(
    val id: UUID,
    val sakId: Long,
    val innhold: Innhold
)