package no.nav.etterlatte.oppgave

import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class OppgaveEndring(
    val id: UUID,
    val oppgaveId: UUID,
    val oppgaveFoer: OppgaveIntern,
    val oppgaveEtter: OppgaveIntern,
    val tidspunkt: Tidspunkt,
) {
    fun sendtTilAttestering(): Boolean =
        oppgaveEtter.erAttestering() &&
            oppgaveFoer.status in listOf(Status.UNDER_BEHANDLING, Status.UNDERKJENT)
}
