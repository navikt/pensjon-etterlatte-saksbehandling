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
    val sakId: Long? = null,
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
    val kilde: OppgaveKilde?,
    val sakId: Long? = null,
    val sakType: SakType? = null,
    val type: OppgaveType,
    val merknad: String? = null,
    val referanse: String? = null,
    val frist: Tidspunkt? = null,
    val saksbehandler: String? = null,
) {
    init {
        require(sakId != null || sakType != null) {
            "SakID eller SakType må være satt"
        }
    }

    fun tilNyInternOppgave(enhet: String) = tilInternOppgave(enhet = enhet)

    fun tilNyInternOppgave(sak: Sak) = tilInternOppgave(sak.ident, sak.id, sak.sakType, sak.enhet)

    private fun tilInternOppgave(
        ident: String? = null,
        sakId: Long? = null,
        sakType: SakType? = null,
        enhet: String,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = enhet,
        kilde = kilde,
        frist =
            frist ?: Tidspunkt
                .now()
                .toLocalDatetimeUTC()
                .plusMonths(1L)
                .toTidspunkt(),
        saksbehandler = saksbehandler?.let { OppgaveSaksbehandler(ident = it) },
        referanse = referanse ?: "",
        opprettet = Tidspunkt.now(),
        sakId = sakId,
        sakType = sakType ?: this.sakType!!,
        fnr = ident,
        type = type,
    )
}

fun opprettNyOppgaveMedReferanseOgSak(
    referanse: String,
    sak: Sak,
    oppgaveKilde: OppgaveKilde?,
    oppgaveType: OppgaveType,
    merknad: String?,
    frist: Tidspunkt? = null,
): OppgaveIntern =
    OppgaveIntern(
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
