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
)

data class BehandlingDetaljer(
    val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDetaljer? = null,
    val ankebehandlingOpprettet: AnkebehandlingOpprettetDetaljer? = null,
    val ankebehandlingAvsluttet: AnkebehandlingAvsluttetDetaljer? = null,
    val behandlingFeilregistrert: BehandlingFeilregistrertDetaljer? = null,
    val ankeITrygderettenbehandlingOpprettet: AnkeITrygderettenbehandlingOpprettetDetaljer? = null,
)

data class KlagebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
)

data class AnkebehandlingOpprettetDetaljer(
    val mottattKlageinstans: LocalDateTime,
)

data class AnkebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
)

data class BehandlingFeilregistrertDetaljer(
    val reason: String,
    val type: KabalSakType,
    val feilregistrert: LocalDateTime,
)

data class AnkeITrygderettenbehandlingOpprettetDetaljer(
    val sendtTilTrygderetten: LocalDateTime,
    val utfall: KlageinstansUtfall?,
)
