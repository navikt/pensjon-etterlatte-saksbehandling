package no.nav.etterlatte.libs.common.oppgave

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class OppgaveKommentarDto(
    val id: UUID = UUID.randomUUID(),
    val sakId: SakId,
    val oppgaveId: UUID,
    val saksbehandler: OppgaveSaksbehandler,
    val kommentar: String,
    val tidspunkt: Tidspunkt,
)

data class OppgaveKommentarRequest(
    val kommentar: String,
)
