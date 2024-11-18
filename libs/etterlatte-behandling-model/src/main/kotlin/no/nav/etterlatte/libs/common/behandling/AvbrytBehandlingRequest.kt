package no.nav.etterlatte.libs.common.behandling

data class AvbrytBehandlingRequest(
    val aarsakTilAvbrytelse: AarsakTilAvbrytelse,
    val kommentar: String? = null,
)

enum class AarsakTilAvbrytelse {
    IKKE_LENGER_AKTUELL,
    SOEKNADEN_ER_IKKE_AKTUELL,
    FEILREGISTRERT,
    AVBRUTT_PAA_GRUNN_AV_FEIL,
    ANNET,
}
