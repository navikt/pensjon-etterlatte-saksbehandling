package no.nav.etterlatte.prosess


import no.nav.etterlatte.libs.common.person.*

fun mockPerson(
    fnr: String = "11057523044",
    foedselsaar: Int = 2010,
    foedselsdato: String? = "2010-04-19",
    doedsdato: String? = null,
    adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.UGRADERT,
    statsborgerskap: String = "NOR",
    foedeland: String = "NOR",
    sivilstatus: String = "ugift",
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