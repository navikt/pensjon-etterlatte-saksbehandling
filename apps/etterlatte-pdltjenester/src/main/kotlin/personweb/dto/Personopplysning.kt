package no.nav.etterlatte.personweb.dto

data class Personopplysning(
    val soeker: Soeker,
)

data class Soeker(
    val fornavn: String,
    val etternavn: String,
)
