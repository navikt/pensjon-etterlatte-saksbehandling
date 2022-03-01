package no.nav.etterlatte


import no.nav.etterlatte.libs.common.person.*
import java.time.LocalDate

fun mockPerson(
    fnr: String = "11057523044",
    foedselsaar: Int = 2010,
    foedselsdato: LocalDate? = LocalDate.parse("2010-04-19"),
    doedsdato: LocalDate? = null,
    adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.UGRADERT,
    statsborgerskap: String = "NOR",
    foedeland: String = "NOR",
    sivilstatus: Sivilstatus = Sivilstatus.UGIFT,
    utland: Utland? = null,
    adresse: Adresse? = null,
    familieRelasjon: FamilieRelasjon? = null,
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
    familieRelasjon = familieRelasjon
)

fun mockNorskAdresse() = Adresse(
    bostedsadresse = Bostedsadresse(
        vegadresse = Vegadresse("Testveien", "4", null, "1234")
    ),
    kontaktadresse = null,
    oppholdsadresse = null,
)

fun mockUgyldigNorskAdresse() = Adresse(
    bostedsadresse = null,
    kontaktadresse = null,
    oppholdsadresse = null,
)