package no.nav.etterlatte.oppgave

import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.Status
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

data class EndringLinje(
    val tittel: String,
    val beskrivelse: String? = null,
)

data class GenerellEndringshendelse(
    val tidspunkt: Tidspunkt,
    val saksbehandler: String?,
    val endringer: List<EndringLinje>,
)

object EndringMapper {
    fun mapOppgaveEndringer(endringer: List<OppgaveEndring>): List<GenerellEndringshendelse> =
        endringer.map {
            GenerellEndringshendelse(
                tidspunkt = it.tidspunkt,
                saksbehandler = null, // Ingen logging på HVEM som har gjort en endring på oppgave. Må fikses
                endringer = tilEndringLinjer(it.oppgaveFoer, it.oppgaveEtter),
            )
        }

    fun mapBehandlingHendelse(hendelse: LagretHendelse): String =
        when (hendelse.hendelse) {
            "BEHANDLING:AVBRUTT" -> "Behandling avbrutt"
            "BEHANDLING:AVKORTET" -> "Behandling avkortet"
            "BEHANDLING:BEREGNET" -> "Behandling beregnet"
            "BEHANDLING:OPPDATERT_GRUNNLAG" -> "Grunnlag oppdatert"
            "BEHANDLING:OPPRETTET" -> "Behandling opprettet"
            "BEHANDLING:TRYGDETID_OPPDATERT" -> "Trygdetid oppdatert"
            "BEHANDLING:VILKAARSVURDERT" -> "Behandling vilkaarsvurdert"
            "GENERELL_BEHANDLING:ATTESTERT" -> "Attestert"
            "GENERELL_BEHANDLING:FATTET" -> "Vedtak fattet"
            "GENERELL_BEHANDLING:OPPRETTET" -> "Generell behandling opprettet"
            "KLAGE:AVBRUTT" -> "Klage avbrutt"
            "KLAGE:FERDIGSTILT" -> "Klage ferdigstilt"
            "KLAGE:OPPRETTET" -> "Klage opprettet"
            "TILBAKEKREVING:OPPRETTET" -> "Tilbakekreving opprettet"
            "VEDTAK:ATTESTERT" -> "Vedtak attestert"
            "VEDTAK:AVSLAG" -> "Vedtak avslag"
            "VEDTAK:FATTET" -> "Vedtak fattet"
            "VEDTAK:IVERKSATT" -> "Vedtak iverksatt"
            "VEDTAK:SAMORDNET" -> "Vedtak samordnet"
            "VEDTAK:TIL_SAMORDNING" -> "Vedtak til samordning"
            "VEDTAK:UNDERKJENT" -> "Vedtak underkjent"
            else -> hendelse.hendelse
        }

    private fun tilEndringLinjer(
        foer: OppgaveIntern,
        etter: OppgaveIntern,
    ): List<EndringLinje> {
        val liste = mutableListOf<EndringLinje>()

        if (foer.status != etter.status) {
            liste.add(
                EndringLinje(
                    "Status endret",
                    "${foer.status.tilLesbarString()} -> ${etter.status.tilLesbarString()}",
                ),
            )
        }
        if (foer.saksbehandler != etter.saksbehandler) {
            liste.add(
                EndringLinje(
                    "Tildelt saksbehandler",
                    "${etter.saksbehandler?.navn?.ellerIngen()}",
                ),
            )
        }
        if (foer.merknad != etter.merknad) {
            liste.add(EndringLinje("Ny merknad", etter.merknad?.ellerIngen()))
        }
        if (foer.enhet != etter.enhet) {
            liste.add(EndringLinje("Flyttet til enhet", "${etter.enhet}"))
        }
        if (foer.frist != etter.frist) {
            liste.add(EndringLinje("Endret frist", "ny frist er ${etter.frist?.toNorskLocalDate()}"))
        }

        return liste
    }

    private fun Status.tilLesbarString() =
        when (this) {
            Status.NY -> "Ny"
            Status.UNDER_BEHANDLING -> "Under behandling"
            Status.PAA_VENT -> "På vent"
            Status.ATTESTERING -> "Til attestering"
            Status.UNDERKJENT -> "Underkjent"
            Status.FERDIGSTILT -> "Ferdigstilt"
            Status.FEILREGISTRERT -> "Feilregistrert"
            Status.AVBRUTT -> "Avbrutt"
        }

    private fun String?.ellerIngen() = if (!this.isNullOrBlank()) this else "Ingen"
}
