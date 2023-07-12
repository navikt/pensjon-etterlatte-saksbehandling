package no.nav.etterlatte.oppgaveny

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
    val saksbehandler: String?,
    val referanse: String?,
    val merknad: String?,
    val opprettet: Tidspunkt,
    val sakType: SakType,
    val fnr: String,
    val frist: Tidspunkt?
)

enum class Status {
    NY,
    UNDER_BEHANDLING,
    FERDIGSTILT
}

enum class OppgaveType {
    FOERSTEGANGSBEHANDLING,
    REVUDERING,
    HENDELSE,
    MANUELT_OPPHOER,
    EKSTERN
}

fun opprettNyOppgaveMedReferanseOgSak(
    referanse: String,
    sak: Sak,
    oppgaveType: OppgaveType
): OppgaveNy {
    return OppgaveNy(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = sak.enhet,
        sakId = sak.id,
        type = oppgaveType,
        saksbehandler = null,
        referanse = referanse,
        merknad = null,
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null
    )
}