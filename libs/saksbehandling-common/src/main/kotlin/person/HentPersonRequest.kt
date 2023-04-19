package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.behandling.SakType

data class HentPersonRequest(
    val foedselsnummer: Folkeregisteridentifikator,
    val rolle: PersonRolle,
    val saktype: SakType
)

data class HentPdlIdentRequest(
    val ident: PersonIdent,
    val saktype: SakType
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