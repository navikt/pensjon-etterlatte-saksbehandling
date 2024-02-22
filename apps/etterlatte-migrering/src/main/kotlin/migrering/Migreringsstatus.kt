package no.nav.etterlatte.migrering

enum class Migreringsstatus {
    HENTA,
    VERIFISERING_FEILA,
    UNDER_MIGRERING,
    UNDER_MIGRERING_MANUELT,
    PAUSE,
    UTBETALING_OK,
    BREVUTSENDING_OK,
    FERDIG,
    MIGRERING_FEILA,
    UTBETALING_FEILA,
    MANUELL,
}
