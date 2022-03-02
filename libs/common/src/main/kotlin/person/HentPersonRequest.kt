package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class HentPersonRequest(
    val foedselsnummer: Foedselsnummer,
    val rolle: PersonRolle = PersonRolle.BARN, // TODO fjerne default arg
)

enum class PersonRolle {
    BARN,
    AVDOED,
    GJENLEVENDE
}