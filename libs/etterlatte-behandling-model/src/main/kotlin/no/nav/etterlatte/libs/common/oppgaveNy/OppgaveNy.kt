package no.nav.etterlatte.libs.common.oppgaveNy

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*

data class OppgaveNy(
    val id: UUID,
    val status: Status,
    val enhet: String,
    val sakId: Long,
    val type: OppgaveType,
    val saksbehandler: String? = null,
    val referanse: String? = null,
    val merknad: String? = null,
    val opprettet: Tidspunkt,
    val sakType: SakType? = null,
    val fnr: String? = null,
    val frist: Tidspunkt?
)

enum class Status {
    NY,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT
}

enum class OppgaveType {
    FOERSTEGANGSBEHANDLING,
    REVUDERING,
    ATTESTERING,
    HENDELSE,
    MANUELT_OPPHOER,
    EKSTERN
}

data class SaksbehandlerEndringDto(
    val oppgaveId: UUID,
    val saksbehandler: String
)

data class FjernSaksbehandlerRequest(
    val oppgaveId: UUID
)

data class OpprettNyOppgaveRequest(
    val referanse: String,
    val sakId: Long,
    val oppgaveType: OppgaveType,
    val saksbehandler: String? = null
)

fun opprettNyOppgaveMedReferanseOgSak(
    referanse: String,
    sak: Sak,
    oppgaveType: OppgaveType,
    saksbehandler: String? = null
): OppgaveNy {
    return OppgaveNy(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = sak.enhet,
        sakId = sak.id,
        type = oppgaveType,
        saksbehandler = saksbehandler,
        referanse = referanse,
        merknad = null,
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null
    )
}