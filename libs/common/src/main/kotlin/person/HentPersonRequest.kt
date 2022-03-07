package no.nav.etterlatte.libs.common.person


data class HentPersonRequest(
    val foedselsnummer: Foedselsnummer,
    val rolle: PersonRolle
)

enum class PersonRolle {
    BARN,
    AVDOED,
    GJENLEVENDE
}