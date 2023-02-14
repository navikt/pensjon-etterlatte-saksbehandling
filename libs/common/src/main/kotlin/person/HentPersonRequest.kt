package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class HentPersonRequest(
    val foedselsnummer: Foedselsnummer,
    val rolle: PersonRolle
)

data class HentFolkeregisterIdentRequest(
    val ident: PersonIdent
)

data class PersonIdent @JsonCreator constructor(
    @JsonProperty val value: String
) {
    override fun toString() =
        if (this.value.length == 11) {
            this.value.replaceRange(6 until 11, "*****")
        } else {
            this.value
        }
}

enum class PersonRolle {
    BARN,
    AVDOED,
    GJENLEVENDE
}