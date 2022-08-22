package no.nav.etterlatte.libs.common.person

data class HentPersonRequest(
    val foedselsnummer: Foedselsnummer,
    val rolle: PersonRolle
)

data class HentFolkeregisterIdentRequest(
    val ident: String
)

enum class PersonRolle {
    BARN,
    AVDOED,
    GJENLEVENDE
}