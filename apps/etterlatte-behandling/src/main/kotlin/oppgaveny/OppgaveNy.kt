package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class OppgaveNy(
    val id: UUID,
    val status: Status,
    val enhet: String,
    val sakId: Long,
    val saksbehandler: String,
    val referanse: String,
    val merknad: String,
    val opprettet: Tidspunkt
)

enum class Status {
    NY,
    UNDER_BEHANDLING,
    FERDIGSTILT
}