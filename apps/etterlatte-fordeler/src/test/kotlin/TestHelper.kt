package no.nav.etterlatte.prosess

import no.nav.etterlatte.libs.common.person.EyBostedsadresse
import no.nav.etterlatte.libs.common.person.EyFamilieRelasjon
import no.nav.etterlatte.libs.common.person.EyVegadresse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Rolle
import no.nav.etterlatte.libs.common.person.eyAdresse
import no.nav.etterlatte.libs.common.person.eyUtland

fun mockPerson(
    rolle: Rolle,
    fnr: String = "11057523044",
    foedselsaar: Int = 2010,
    foedselsdato: String? = "2010-04-19",
    doedsdato: String? = null,
    adressebeskyttelse: Boolean = false,
    statsborgerskap: String = "NOR",
    foedeland: String = "NOR",
    sivilstatus: String = "ugift",
    utland: eyUtland? = null,
    adresse: eyAdresse? = null,
    familieRelasjon: EyFamilieRelasjon? = null,
) = Person(
    fornavn = "Ola",
    etternavn = "Nordmann",
    foedselsnummer = Foedselsnummer.of(fnr),
    foedselsaar = foedselsaar,
    foedselsdato = foedselsdato,
    doedsdato = doedsdato,
    adressebeskyttelse = adressebeskyttelse,
    adresse = adresse,
    statsborgerskap = statsborgerskap,
    foedeland = foedeland,
    sivilstatus = sivilstatus,
    utland = utland,
    familieRelasjon = familieRelasjon,
    rolle = rolle
)

fun mockNorskAdresse() = eyAdresse(
    bostedsadresse = EyBostedsadresse(
        vegadresse = EyVegadresse("Testveien", "4", null, "1234")
    ),
    kontaktadresse = null,
    oppholdsadresse = null,
)

fun mockUgyldigNorskAdresse() = eyAdresse(
    bostedsadresse = null,
    kontaktadresse = null,
    oppholdsadresse = null,
)