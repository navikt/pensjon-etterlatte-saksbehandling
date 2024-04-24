package no.nav.etterlatte.rapidsandrivers

enum class Behandlingssteg {
    KLAR,
    BEHANDLING_OPPRETTA,
    VILKAARSVURDERT,
    TRYGDETID_OPPRETTA,
    BEREGNA,
    AVKORTA,
    VEDTAK_FATTA,
    IVERKSATT,
    ;

    companion object {
        const val KEY = "BEHANDLINGSSTEG"
    }
}
