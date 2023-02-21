package no.nav.etterlatte.libs.common.behandling
data class Persongalleri(
    val soeker: String,
    val innsender: String? = null,
    val soesken: List<String> = emptyList(),
    val avdoed: List<String> = emptyList(),
    val gjenlevende: List<String> = emptyList()
)