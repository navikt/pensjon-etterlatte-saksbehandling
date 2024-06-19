package no.nav.etterlatte.personweb.dto

import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.LocalDate

data class PersonNavnFoedselsaar(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val foedselsaar: Int,
    val foedselsdato: LocalDate? = null,
)

data class PersonSoekSvar(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val foedselsnummer: String,
    val bostedsadresse: List<Adresse>?,
)
