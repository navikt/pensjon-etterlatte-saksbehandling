package no.nav.etterlatte.migrering

enum class Migreringsstatus {
    HENTA,
    VERIFISERING_FEILA,
    UNDER_MIGRERING,
    UNDER_MIGRERING_MANUELT,
    PAUSE,
    FERDIG,
    MIGRERING_FEILA,
    UTBETALING_FEILA,
    MANUELL,
}
