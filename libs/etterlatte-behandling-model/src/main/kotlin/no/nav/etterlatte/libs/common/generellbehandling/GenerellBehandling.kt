package no.nav.etterlatte.libs.common.generellbehandling

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class GenerellBehandling(
    val id: UUID,
    val sakId: Long,
    val innhold: Innhold,
    val opprettet: Tidspunkt
)