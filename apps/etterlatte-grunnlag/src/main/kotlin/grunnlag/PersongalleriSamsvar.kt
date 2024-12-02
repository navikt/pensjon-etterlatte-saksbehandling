package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.behandling.Persongalleri

data class PersongalleriSamsvar(
    val persongalleri: Persongalleri,
    val kilde: GenerellKilde,
    val persongalleriPdl: Persongalleri?,
    val kildePdl: GenerellKilde?,
    val problemer: List<MismatchPersongalleri>,
)

enum class MismatchPersongalleri {
    ENDRET_SOEKER_FNR,

    MANGLER_GJENLEVENDE,
    MANGLER_AVDOED,
    MANGLER_SOESKEN,

    EKSTRA_GJENLEVENDE,
    EKSTRA_AVDOED,
    EKSTRA_SOESKEN,

    HAR_PERSONER_UTEN_IDENTER,
}
