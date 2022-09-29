package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

data class Navn(
    val fornavn: String,
    val etternavn: String
) {
    override fun toString(): String = "$fornavn $etternavn"
}