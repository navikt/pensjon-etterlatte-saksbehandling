package no.nav.etterlatte

import no.nav.etterlatte.fordeler.FordelerRiverServiceTest
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.LocalDateTime

const val SVERIGE = "SWE"
const val NORGE = "NOR"

fun mockPerson(
    fnr: String = AVDOED_FOEDSELSNUMMER.value,
    foedselsaar: Int = 2010,
    foedselsdato: LocalDate? = LocalDate.parse("2010-04-19"),
    doedsdato: LocalDate? = null,
    adressebeskyttelse: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
    statsborgerskap: String = NORGE,
    foedeland: String = NORGE,
    sivilstatus: Sivilstatus = Sivilstatus.UGIFT,
    sivilstand: Sivilstand? = null,
    utland: Utland? = null,
    bostedsadresse: Adresse? = null,
    familieRelasjon: FamilieRelasjon? = null,
    vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>? = null,
) = Person(
    fornavn = "Ola",
    etternavn = "Nordmann",
    foedselsnummer = Folkeregisteridentifikator.of(fnr),
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
    sivilstand = sivilstand?.let { listOf(sivilstand) } ?: emptyList(),
    utland = utland,
    familieRelasjon = familieRelasjon,
    avdoedesBarn = null,
    avdoedesBarnUtenIdent = null,
    vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt,
    pdlStatsborgerskap = null,
)

fun mockNorskAdresse(
    adresseLinje1: String = "Testveien 4",
    gyldigTilOgMed: LocalDateTime? = null,
) = Adresse(
    type = AdresseType.VEGADRESSE,
    aktiv = true,
    coAdresseNavn = null,
    adresseLinje1 = adresseLinje1,
    adresseLinje2 = null,
    adresseLinje3 = null,
    postnr = "1234",
    poststed = null,
    land = null,
    kilde = "FREG",
    gyldigFraOgMed = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
    gyldigTilOgMed = gyldigTilOgMed,
)

fun mockUgyldigAdresse(
    type: AdresseType = AdresseType.UKJENT_BOSTED,
    gyldigFraOgMed: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
    gyldigTilOgMed: LocalDateTime? = null,
) = Adresse(
    type = type,
    aktiv = true,
    coAdresseNavn = null,
    adresseLinje1 = "Tull",
    adresseLinje2 = null,
    adresseLinje3 = null,
    postnr = null,
    poststed = null,
    land = null,
    kilde = "FREG",
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = gyldigTilOgMed,
)

fun readSoknad(file: String): Barnepensjon {
    val skjemaInfo = objectMapper.writeValueAsString(objectMapper.readTree(readFile(file)).get("@skjema_info"))
    return objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)
}

fun readFile(file: String) =
    FordelerRiverServiceTest::class.java.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")
