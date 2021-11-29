package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.person.Foedselsnummer

data class Person(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer,
    val foedselsaar: Int?,
    val foedselsdato: String?,
    val adressebeskyttelse: Boolean,
    val adresse: String?,
    val husnummer: String?,
    val husbokstav: String?,
    val postnummer: String?,
    val poststed: String?,
    val statsborgerskap: String?,
    val sivilstatus: String?
)
