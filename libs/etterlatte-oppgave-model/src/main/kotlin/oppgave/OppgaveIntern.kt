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
    abstract val saksbehandler: OppgaveSaksbehandler?
    abstract val opprettet: Tidspunkt
    abstract val sakType: SakType
    abstract val fnr: String?
    abstract val frist: Tidspunkt?
}

data class OppgaveSaksbehandler(
    val ident: String,
    val navn: String? = null,
)

data class OppgaveIntern(
    val id: UUID,
    override val status: Status,
    override val enhet: String,
    val sakId: Long,
    val kilde: OppgaveKilde? = null,
    override val type: OppgaveType,
    override val saksbehandler: OppgaveSaksbehandler? = null,
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

    fun erAvsluttet(): Boolean = status.erAvsluttet()

    fun erFerdigstilt(): Boolean = status.erFerdigstilt()

    fun erAttestering(): Boolean {
        return OppgaveType.ATTESTERING === type
    }
}

data class OppgaveListe(val sak: Sak, val oppgaver: List<OppgaveIntern>)

data class GosysOppgave(
    val id: Long,
    val versjon: Long,
    override val status: Status,
    override val saksbehandler: OppgaveSaksbehandler?,
    override val enhet: String,
    override val opprettet: Tidspunkt,
    override val frist: Tidspunkt?,
    override val sakType: SakType,
    override val fnr: String? = null,
    val gjelder: String,
    val beskrivelse: String?,
    val journalpostId: String?,
) : Oppgave() {
    override val type: OppgaveType
        get() = OppgaveType.GOSYS
}

enum class Status {
    NY,
    UNDER_BEHANDLING,
    PAA_VENT,
    FERDIGSTILT,
    FEILREGISTRERT,
    AVBRUTT,
    ;

    fun erAvsluttet(): Boolean {
        return when (this) {
            NY,
            UNDER_BEHANDLING,
            PAA_VENT,
            -> false

            FERDIGSTILT,
            FEILREGISTRERT,
            AVBRUTT,
            -> true
        }
    }

    fun erFerdigstilt(): Boolean = this === FERDIGSTILT
}

enum class OppgaveKilde {
    HENDELSE,
    BEHANDLING,
    GENERELL_BEHANDLING,
    EKSTERN,
    TILBAKEKREVING,
    GJENOPPRETTING,
    SAKSBEHANDLER,
}

enum class OppgaveType {
    FOERSTEGANGSBEHANDLING,
    REVURDERING,
    VURDER_KONSEKVENS,
    ATTESTERING,
    UNDERKJENT,
    GOSYS,
    KRAVPAKKE_UTLAND,
    KLAGE,
    TILBAKEKREVING,
    OMGJOERING,
    JOURNALFOERING,
    GJENOPPRETTING_ALDERSOVERGANG, // Saker som ble opphørt i Pesys etter 18 år gammel regelverk
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

data class SettPaaVentRequest(
    val merknad: String,
    val status: Status,
)

data class FerdigstillRequest(
    val merknad: String?,
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
    val frist: Tidspunkt? = null,
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
