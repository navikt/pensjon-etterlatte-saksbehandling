package no.nav.etterlatte.libs.common.oppgaveNy

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import java.util.*

abstract class Oppgave {
    abstract val status: Status
    abstract val type: OppgaveType
    abstract val enhet: String
    abstract val saksbehandler: String?
    abstract val opprettet: Tidspunkt
    abstract val sakType: SakType
    abstract val fnr: String?
    abstract val frist: Tidspunkt?
}

data class OppgaveNy(
    val id: UUID,
    override val status: Status,
    override val enhet: String,
    val sakId: Long,
    val kilde: OppgaveKilde? = null,
    override val type: OppgaveType,
    override val saksbehandler: String? = null,
    val referanse: String,
    val merknad: String? = null,
    override val opprettet: Tidspunkt,
    override val sakType: SakType,
    override val fnr: String? = null,
    override val frist: Tidspunkt?
) : Oppgave() {
    fun erAvsluttet(): Boolean {
        return Status.erAvsluttet(this.status)
    }
}

data class GosysOppgave(
    val id: Long,
    val versjon: Long,
    override val status: Status,
    override val saksbehandler: String?,
    override val enhet: String,
    override val opprettet: Tidspunkt,
    override val frist: Tidspunkt?,
    override val sakType: SakType,
    override val fnr: String,
    val gjelder: String,
    val beskrivelse: String
) : Oppgave() {
    override val type: OppgaveType
        get() = OppgaveType.GOSYS
}

enum class Status {
    NY,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT,
    AVBRUTT;

    companion object {
        fun erAvsluttet(status: Status): Boolean {
            return when (status) {
                NY,
                UNDER_BEHANDLING -> false
                FERDIGSTILT,
                FEILREGISTRERT,
                AVBRUTT -> true
            }
        }
    }
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
    UNDERKJENT,
    GOSYS
}

data class SaksbehandlerEndringDto(
    val saksbehandler: String,
    val versjon: Long? = null
)

data class RedigerFristRequest(
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
    oppgaveType: OppgaveType,
    merknad: String?
): OppgaveNy {
    return OppgaveNy(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = sak.enhet,
        sakId = sak.id,
        kilde = oppgaveKilde,
        saksbehandler = null,
        referanse = referanse,
        merknad = merknad,
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null,
        type = oppgaveType
    )
}