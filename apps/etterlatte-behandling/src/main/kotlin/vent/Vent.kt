package no.nav.etterlatte.vent

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class Vent(
    val id: UUID,
    val oppgaveId: UUID,
    val behandlingId: UUID,
    val ventetype: Ventetype,
    val paaVentTil: Tidspunkt,
)

enum class Ventetype {
    VARSLING,
}
