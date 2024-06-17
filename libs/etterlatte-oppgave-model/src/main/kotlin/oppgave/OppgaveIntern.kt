package no.nav.etterlatte.libs.common.oppgave

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import java.time.YearMonth
import java.util.UUID

data class OppgaveSaksbehandler(
    val ident: String,
    val navn: String? = null,
)

data class OppgaveIntern(
    val id: UUID,
    val status: Status,
    val enhet: String,
    val sakId: Long,
    val kilde: OppgaveKilde? = null,
    val type: OppgaveType,
    val saksbehandler: OppgaveSaksbehandler? = null,
    val forrigeSaksbehandlerIdent: String? = null,
    val referanse: String,
    val merknad: String? = null,
    val opprettet: Tidspunkt,
    val sakType: SakType,
    val fnr: String? = null,
    val frist: Tidspunkt?,
) {
    fun manglerSaksbehandler(): Boolean = saksbehandler == null

    fun erAvsluttet(): Boolean = status.erAvsluttet()

    fun erUnderBehandling() = status.erUnderBehandling()

    fun erAttestering(): Boolean = status == Status.ATTESTERING

    fun typeKanAttesteres() =
        type in
            listOf(
                OppgaveType.FOERSTEGANGSBEHANDLING,
                OppgaveType.REVURDERING,
                OppgaveType.KRAVPAKKE_UTLAND,
                OppgaveType.TILBAKEKREVING,
                OppgaveType.KLAGE,
            )
}

data class OppgavebenkStats(
    val antallOppgavelistaOppgaver: Long,
    val antallMinOppgavelisteOppgaver: Long,
)

enum class Status {
    NY,
    UNDER_BEHANDLING,
    PAA_VENT,
    ATTESTERING,
    UNDERKJENT,
    FERDIGSTILT,
    FEILREGISTRERT,
    AVBRUTT,
    ;

    fun erAvsluttet(): Boolean =
        when (this) {
            NY,
            UNDER_BEHANDLING,
            PAA_VENT,
            ATTESTERING,
            UNDERKJENT,
            -> false

            FERDIGSTILT,
            FEILREGISTRERT,
            AVBRUTT,
            -> true
        }

    // TODO: Gå gjennom navngiving her. Gir det mening med "under behandling" som status OG samlebegrep...?
    fun erUnderBehandling(): Boolean = this in listOf(UNDER_BEHANDLING, PAA_VENT, ATTESTERING, UNDERKJENT)

    fun erFerdigstilt(): Boolean = this == FERDIGSTILT

    companion object {
        fun skalOmregnesVedGRegulering() =
            listOf(
                ATTESTERING,
            )
    }
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
    GOSYS,
    KRAVPAKKE_UTLAND,
    KLAGE,
    TILBAKEKREVING,
    OMGJOERING,
    JOURNALFOERING,
    GJENOPPRETTING_ALDERSOVERGANG, // Saker som ble opphørt i Pesys etter 18 år gammel regelverk
    AKTIVITETSPLIKT,
    AKTIVITETSPLIKT_REVURDERING,
    ;

    companion object {
        fun fra(behandlingType: BehandlingType) =
            when (behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> FOERSTEGANGSBEHANDLING
                BehandlingType.REVURDERING -> REVURDERING
            }
    }
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
    val opphoerFraOgMed: YearMonth? = null,
)

data class NyOppgaveDto(
    val oppgaveKilde: OppgaveKilde?,
    val oppgaveType: OppgaveType,
    val merknad: String?,
    val referanse: String? = null,
    val frist: Tidspunkt? = null,
    val saksbehandler: String? = null,
)

fun opprettNyOppgaveMedReferanseOgSak(
    referanse: String,
    sak: Sak,
    kilde: OppgaveKilde?,
    type: OppgaveType,
    merknad: String?,
    frist: Tidspunkt? = null,
    saksbehandler: String? = null,
): OppgaveIntern {
    val opprettet = Tidspunkt.now()

    val oppgaveFrist =
        frist ?: opprettet
            .toLocalDatetimeUTC()
            .plusMonths(1L)
            .toTidspunkt()

    return OppgaveIntern(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = sak.enhet,
        sakId = sak.id,
        kilde = kilde,
        saksbehandler = saksbehandler?.let { OppgaveSaksbehandler(ident = it) },
        referanse = referanse,
        merknad = merknad,
        opprettet = opprettet,
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = oppgaveFrist,
        type = type,
    )
}
