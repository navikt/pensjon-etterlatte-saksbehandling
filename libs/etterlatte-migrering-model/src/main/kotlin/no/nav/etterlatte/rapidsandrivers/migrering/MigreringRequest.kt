package no.nav.etterlatte.rapidsandrivers.migrering

enum class MigreringKjoringVariant {
    FULL_KJORING,
    MED_PAUSE,
    FORTSETT_ETTER_PAUSE,
}

data class PesysId(
    val id: Long,
)
