package personweb.dto

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

data class PersonNavn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
)
