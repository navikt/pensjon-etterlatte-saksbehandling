package no.nav.etterlatte.oppgaveny

import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class OppgaveEndring(
    val id: UUID,
    val oppgaveId: UUID,
    val oppgaveFoer: OppgaveNy,
    val oppgaveEtter: OppgaveNy,
    val tidspunkt: Tidspunkt
)