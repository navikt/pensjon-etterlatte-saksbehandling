package no.nav.etterlatte.libs.common.behandling

data class Navn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
) {
    override fun toString(): String = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")
}
