package grunnlag

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle

data class PersonMedNavn(
    val fnr: Folkeregisteridentifikator,
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
    val rolle: PersonRolle
)