package no.nav.etterlatte


import no.nav.etterlatte.libs.common.person.*
import java.time.LocalDate
import java.time.LocalDateTime

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
    bostedsadresse: Adresse? = null,
    familieRelasjon: FamilieRelasjon? = null,
) = Person(
    fornavn = "Ola",
    etternavn = "Nordmann",
    foedselsnummer = Foedselsnummer.of(fnr),
    foedselsaar = foedselsaar,
    foedselsdato = foedselsdato,
    doedsdato = doedsdato,
    adressebeskyttelse = adressebeskyttelse,
    bostedsadresse = bostedsadresse?.let { listOf(bostedsadresse) } ?: emptyList(),
    deltBostedsadresse = emptyList(),
    oppholdsadresse = emptyList(),
    kontaktadresse = emptyList(),
    statsborgerskap = statsborgerskap,
    foedeland = foedeland,
    sivilstatus = sivilstatus,
    utland = utland,
    familieRelasjon = familieRelasjon
)

fun mockNorskAdresse() = Adresse(
    type = AdresseType.VEGADRESSE,
    aktiv = true,
    coAdresseNavn = null,
    adresseLinje1 = "Testveien 4",
    adresseLinje2 = null,
    adresseLinje3 = null,
    postnr = "1234",
    poststed = null,
    land = null,
    kilde = "FREG",
    gyldigFraOgMed = LocalDateTime.now().minusYears(1),
    gyldigTilOgMed = null
)

fun mockUgyldigAdresse() = Adresse(
    type = AdresseType.UKJENT_BOSTED,
    aktiv = true,
    coAdresseNavn = null,
    adresseLinje1 = "Tull",
    adresseLinje2 = null,
    adresseLinje3 = null,
    postnr = null,
    poststed = null,
    land = null,
    kilde = "FREG",
    gyldigFraOgMed = LocalDateTime.now().minusYears(1),
    gyldigTilOgMed = null
)