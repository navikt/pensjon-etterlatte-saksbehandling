package no.nav.etterlatte.libs.common.oppgave

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import java.util.UUID

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

data class OppgaveIntern(
    val id: UUID,
    override val status: Status,
    override val enhet: String,
    val sakId: Long,
    val kilde: OppgaveKilde? = null,
    override val type: OppgaveType,
    override val saksbehandler: String? = null,
    val saksbehandlerNavn: String? = null,
    val referanse: String,
    val merknad: String? = null,
    override val opprettet: Tidspunkt,
    override val sakType: SakType,
    override val fnr: String? = null,
    override val frist: Tidspunkt?,
) : Oppgave() {
    fun manglerSaksbehandler(): Boolean {
        return saksbehandler == null
    }

    fun harSaksbehandler(): Boolean {
        return saksbehandler != null
    }

    fun erAvsluttet(): Boolean {
        return Status.erAvsluttet(this.status)
    }

    fun erFerdigstilt(): Boolean {
        return Status.erFerdigstilt(this.status)
    }

    fun erAttestering(): Boolean {
        return OppgaveType.ATTESTERING === type
    }
}

data class OppgaveListe(val sak: Sak, val oppgaver: List<OppgaveIntern>)

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
    val beskrivelse: String,
) : Oppgave() {
    override val type: OppgaveType
        get() = OppgaveType.GOSYS
}

enum class Status {
    NY,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT,
    AVBRUTT,
    ;

    companion object {
        fun erAvsluttet(status: Status): Boolean {
            return when (status) {
                NY,
                UNDER_BEHANDLING,
                -> false
                FERDIGSTILT,
                FEILREGISTRERT,
                AVBRUTT,
                -> true
            }
        }

        fun erFerdigstilt(status: Status): Boolean {
            return status === FERDIGSTILT
        }

        fun erUnderbehandling(status: Status): Boolean {
            return status === UNDER_BEHANDLING
        }
    }
}

enum class OppgaveKilde {
    HENDELSE,
    BEHANDLING,
    GENERELL_BEHANDLING,
    EKSTERN,
    TILBAKEKREVING,
}

enum class OppgaveType {
    FOERSTEGANGSBEHANDLING,
    REVURDERING,
    MANUELT_OPPHOER,
    VURDER_KONSEKVENS,
    ATTESTERING,
    UNDERKJENT,
    GOSYS,
    KRAVPAKKE_UTLAND,
    KLAGE,
    TILBAKEKREVING,
    OMGJOERING,
    JOURNALFOERING,
}

data class SaksbehandlerEndringDto(
    val saksbehandler: String,
)

data class SaksbehandlerEndringGosysDto(
    val saksbehandler: String,
    val versjon: Long,
)

data class RedigerFristRequest(
    val frist: Tidspunkt,
)

data class RedigerFristGosysRequest(
    val frist: Tidspunkt,
    val versjon: Long,
)

data class SakIdOgReferanse(
    val sakId: Long,
    val referanse: String,
)

data class VedtakEndringDTO(
    val sakIdOgReferanse: SakIdOgReferanse,
    val vedtakHendelse: VedtakHendelse,
    val vedtakType: VedtakType,
)

data class NyOppgaveDto(
    val oppgaveKilde: OppgaveKilde?,
    val oppgaveType: OppgaveType,
    val merknad: String?,
    val referanse: String? = null,
)

fun opprettNyOppgaveMedReferanseOgSak(
    referanse: String,
    sak: Sak,
    oppgaveKilde: OppgaveKilde?,
    oppgaveType: OppgaveType,
    merknad: String?,
    frist: Tidspunkt? = null,
): OppgaveIntern {
    return OppgaveIntern(
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
        frist = frist,
        type = oppgaveType,
    )
}
