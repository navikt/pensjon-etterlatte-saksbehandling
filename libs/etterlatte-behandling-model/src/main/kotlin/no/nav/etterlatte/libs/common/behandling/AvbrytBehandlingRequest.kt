package no.nav.etterlatte.libs.common.behandling

data class AvbrytBehandlingRequest(
    val aarsakTilAvbrytelse: AarsakTilAvbrytelse,
    val kommentar: String? = null,
)

sealed class AarsakTilAvbrytelse {
    enum class Foerstegangsbehandling {
        SOEKNADEN_ER_IKKE_AKTUELL,
        FEILREGISTRERT,
        ANNET,
    }

    enum class Revurdering {
        IKKE_LENGER_AKTUELL,
        FEILREGISTRERT,
        AVBRUTT_PAA_GRUNN_AV_FEIL,
        ANNET,
    }
}
