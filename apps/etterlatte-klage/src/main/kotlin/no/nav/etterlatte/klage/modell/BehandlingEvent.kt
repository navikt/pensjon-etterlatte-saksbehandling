package no.nav.etterlatte.klage.modell

import java.time.LocalDateTime
import java.util.UUID

// se no.nav.familie.klage.kabal.OversendtKlageAnkeV3
data class BehandlingEvent(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
    val kabalReferanse: String,
    val type: BehandlingEventType,
    val detaljer: BehandlingDetaljer,
) {
    fun mottattEllerAvsluttetTidspunkt(): LocalDateTime {
        val feilmelding = "Burde hatt behandlingdetaljer for event fra kabal av type $type"
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                detaljer.klagebehandlingAvsluttet?.avsluttet ?: throw Feil(feilmelding)

            BehandlingEventType.ANKEBEHANDLING_OPPRETTET ->
                detaljer.ankebehandlingOpprettet?.mottattKlageinstans ?: throw Feil(feilmelding)

            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                detaljer.ankebehandlingAvsluttet?.avsluttet ?: throw Feil(
                    feilmelding,
                )

            BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET ->
                detaljer.ankeITrygderettenbehandlingOpprettet?.sendtTilTrygderetten ?: throw Feil(feilmelding)

            BehandlingEventType.BEHANDLING_FEILREGISTRERT ->
                detaljer.behandlingFeilregistrert?.feilregistrert
                    ?: throw Feil("Fant ikke tidspunkt for feilregistrering")
        }
    }

    fun utfall(): KlageinstansUtfall? {
        val feilmelding = "Burde hatt behandlingdetaljer for event fra kabal av type $type"
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                detaljer.klagebehandlingAvsluttet?.utfall ?: throw Feil(
                    feilmelding,
                )

            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                detaljer.ankebehandlingAvsluttet?.utfall ?: throw Feil(
                    feilmelding,
                )

            else -> null
        }
    }

    fun journalpostReferanser(): List<String> {
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                detaljer.klagebehandlingAvsluttet?.journalpostReferanser
                    ?: listOf()

            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                detaljer.ankebehandlingAvsluttet?.journalpostReferanser
                    ?: listOf()

            else -> listOf()
        }
    }
}

data class BehandlingDetaljer(
    val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDetaljer? = null,
    val ankebehandlingOpprettet: AnkebehandlingOpprettetDetaljer? = null,
    val ankebehandlingAvsluttet: AnkebehandlingAvsluttetDetaljer? = null,
    val behandlingFeilregistrert: BehandlingFeilregistrertDetaljer? = null,
    val ankeITrygderettenbehandlingOpprettet: AnkeITrygderettenbehandlingOpprettetDetaljer? = null,
) {
    fun journalpostReferanser(): List<String> {
        return klagebehandlingAvsluttet?.journalpostReferanser ?: ankebehandlingAvsluttet?.journalpostReferanser
            ?: listOf()
    }

    fun oppgaveTekst(): String {
        return klagebehandlingAvsluttet?.oppgaveTekst()
            ?: ankebehandlingOpprettet?.oppgaveTekst()
            ?: ankebehandlingAvsluttet?.oppgaveTekst()
            ?: "Ukjent"
    }
}

data class KlagebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(): String {
        return "Hendelse fra klage av type klagebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
    }
}

data class AnkebehandlingOpprettetDetaljer(
    val mottattKlageinstans: LocalDateTime,
) {
    fun oppgaveTekst(): String = "Hendelse fra klage av type ankebehandling opprettet mottatt. Mottatt klageinstans: $mottattKlageinstans."
}

data class AnkebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(): String =
        "Hendelse fra klage av type ankebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class BehandlingFeilregistrertDetaljer(val reason: String, val type: Type, val feilregistrert: LocalDateTime)

data class AnkeITrygderettenbehandlingOpprettetDetaljer(
    val sendtTilTrygderetten: LocalDateTime,
    val utfall: KlageinstansUtfall?,
)
