package no.nav.etterlatte.libs.common.oppgaveNy

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import java.util.*

data class OppgaveNy(
    val id: UUID,
    val status: Status,
    val enhet: String,
    val sakId: Long,
    val kilde: OppgaveKilde? = null,
    val type: OppgaveType,
    val saksbehandler: String? = null,
    val referanse: String? = null,
    val merknad: String? = null,
    val opprettet: Tidspunkt,
    val sakType: SakType,
    val fnr: String? = null,
    val frist: Tidspunkt?
)

enum class Status {
    NY,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT,
    AVBRUTT
}

enum class OppgaveKilde {
    HENDELSE,
    BEHANDLING,
    EKSTERN
}

enum class OppgaveType {
    FOERSTEGANGSBEHANDLING,
    REVURDERING,
    MANUELT_OPPHOER,
    VURDER_KONSEKVENS,
    ATTESTERING,
    UNDERKJENT
}

data class SaksbehandlerEndringDto(
    val oppgaveId: UUID,
    val saksbehandler: String
)

data class FjernSaksbehandlerRequest(
    val oppgaveId: UUID
)

data class RedigerFristRequest(
    val oppgaveId: UUID,
    val frist: Tidspunkt
)

data class VedtakOppgaveDTO(
    val sakId: Long,
    val referanse: String
)

data class VedtakEndringDTO(
    val vedtakOppgaveDTO: VedtakOppgaveDTO,
    val vedtakHendelse: VedtakHendelse
)

fun opprettNyOppgaveMedReferanseOgSak(
    referanse: String,
    sak: Sak,
    oppgaveKilde: OppgaveKilde?,
    oppgaveType: OppgaveType
): OppgaveNy {
    return OppgaveNy(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = sak.enhet,
        sakId = sak.id,
        kilde = oppgaveKilde,
        saksbehandler = null,
        referanse = referanse,
        merknad = null,
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null,
        type = oppgaveType
    )
}