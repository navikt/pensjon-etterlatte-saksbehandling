package no.nav.etterlatte.personweb.dto

import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import java.time.LocalDate

data class PersonNavnFoedselsaar(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val historiskeFoedselsnummer: List<Folkeregisteridentifikator>,
    val foedselsaar: Int,
    val foedselsdato: LocalDate? = null,
    val doedsdato: LocalDate? = null,
    val vergemaal: VergemaalEllerFremtidsfullmakt? = null,
)

data class PersonSoekSvar(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val foedselsnummer: String,
    val bostedsadresse: List<Adresse>?,
)
