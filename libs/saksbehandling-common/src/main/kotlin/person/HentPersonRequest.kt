package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.behandling.SakType

data class HentPersonRequest(
    val foedselsnummer: Folkeregisteridentifikator,
    val rolle: PersonRolle,
    val saktype: SakType,
)

data class HentPersongalleriRequest(
    val mottakerAvYtelsen: Folkeregisteridentifikator,
    val innsender: Folkeregisteridentifikator?,
    val saktype: SakType,
)

data class HentPdlIdentRequest(
    val ident: PersonIdent,
)

// TODO: må få med sakstype her og endre til folkeregisteridentifikator
data class HentAdressebeskyttelseRequest(
    val ident: PersonIdent,
)

data class PersonIdent
    @JsonCreator
    constructor(
        @JsonProperty val value: String,
    ) {
        override fun toString() =
            if (this.value.length == 11) {
                this.value.replaceRange(6 until 11, "*****")
            } else {
                this.value
            }
    }
