package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.person.Foedselsnummer

data class Person(
    val fornavn: String,
    val etternavn: String,
    private val foedselsnummer: String,
    val foedselsaar: Int?,
    val foedselsdato: String?,
    val adressebeskyttelse: Boolean,
    val adresse: String?,
    val husnummer: String?,
    val husbokstav: String?,
    val postnummer: String?,
    val poststed: String?,
    val statsborgerskap: String?,
    val sivilstatus: String?,
    val fnr: Foedselsnummer = Foedselsnummer.of(foedselsnummer)
)
