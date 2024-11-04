package no.nav.etterlatte.libs.testdata.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.ADRESSEBESKYTTELSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.DOEDSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FAMILIERELASJON
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSAAR
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONROLLE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SIVILSTATUS
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.pdl.personTestData
import java.time.LocalDateTime
import java.util.UUID.randomUUID

val kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")
val statiskUuid = randomUUID()!!

val AVDOED_FOEDSELSNUMMER = Folkeregisteridentifikator.of("08498224343") // Gjennomsiktig Klemme
val AVDOED2_FOEDSELSNUMMER = Folkeregisteridentifikator.of("16508201382") // Kunnskapsrik Kråkebolle
val GJENLEVENDE_FOEDSELSNUMMER = Folkeregisteridentifikator.of("01498344336") // Kunst Løvinne
val SOEKER_FOEDSELSNUMMER = Folkeregisteridentifikator.of("25478323363") // Rød Blanding
val SOEKER2_FOEDSELSNUMMER = Folkeregisteridentifikator.of("09438336165") // Kry Vits
val INNSENDER_FOEDSELSNUMMER = Folkeregisteridentifikator.of("18498248795") // Vrien Netthinne
val HELSOESKEN_FOEDSELSNUMMER = Folkeregisteridentifikator.of("16478313601") // Nødvendig Dugnad
val HELSOESKEN2_FOEDSELSNUMMER = Folkeregisteridentifikator.of("09508229892") // Tru Panjabi
val HELSOESKEN3_FOEDSELSNUMMER = Folkeregisteridentifikator.of("17418340118") // Kontant Kjede
val HALVSOESKEN_FOEDSELSNUMMER = Folkeregisteridentifikator.of("27458328671") // Gjestfri Geometri
val HALVSOESKEN_ANNEN_FORELDER = Folkeregisteridentifikator.of("31488338237") // Ulogisk Bensin
val VERGE_FOEDSELSNUMMER = Folkeregisteridentifikator.of("02438311109") // Vrien Påske
val BARN_FOEDSELSNUMMER = Folkeregisteridentifikator.of("08481376816") // Spiss Bjørk

val ADRESSE_DEFAULT =
    listOf(
        Adresse(
            type = AdresseType.VEGADRESSE,
            aktiv = true,
            adresseLinje1 = "Ulsetløkkja",
            adresseLinje2 = null,
            adresseLinje3 = null,
            coAdresseNavn = null,
            postnr = "2512",
            poststed = "KVIKNE",
            land = null,
            kilde = "pdl",
            gyldigFraOgMed = LocalDateTime.parse("2012-02-16T00:00"),
            gyldigTilOgMed = null,
        ),
    )

internal val soekerTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> =
    mapOf(
        NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Søker", "mellom", "Barn").toJsonNode()),
        FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, SOEKER_FOEDSELSNUMMER.toJsonNode()),
        FOEDSELSDATO to Opplysning.Konstant(randomUUID(), kilde, SOEKER_FOEDSELSNUMMER.getBirthDate().toJsonNode()),
        FOEDSELSAAR to Opplysning.Konstant(randomUUID(), kilde, SOEKER_FOEDSELSNUMMER.getBirthDate().year.toJsonNode()),
        FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        ADRESSEBESKYTTELSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                AdressebeskyttelseGradering.UGRADERT.toJsonNode(),
            ),
        BOSTEDSADRESSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde = kilde,
                verdi = ADRESSE_DEFAULT.toJsonNode(),
            ),
        STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.BARN.toJsonNode()),
        FAMILIERELASJON to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                FamilieRelasjon(
                    ansvarligeForeldre = listOf(AVDOED_FOEDSELSNUMMER, GJENLEVENDE_FOEDSELSNUMMER),
                    foreldre = listOf(AVDOED_FOEDSELSNUMMER, GJENLEVENDE_FOEDSELSNUMMER),
                    barn = null,
                ).toJsonNode(),
            ),
        Opplysningstype.SOEKER_PDL_V1 to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                soeker().toJsonNode(),
            ),
    )

internal val soeskenTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> =
    mapOf(
        NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Hel", "mellom", "Søsken").toJsonNode()),
        FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, HELSOESKEN_FOEDSELSNUMMER.toJsonNode()),
        FOEDSELSDATO to Opplysning.Konstant(randomUUID(), kilde, HELSOESKEN_FOEDSELSNUMMER.getBirthDate().toJsonNode()),
        FOEDSELSAAR to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                HELSOESKEN_FOEDSELSNUMMER.getBirthDate().year.toJsonNode(),
            ),
        FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        ADRESSEBESKYTTELSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                AdressebeskyttelseGradering.UGRADERT.toJsonNode(),
            ),
        BOSTEDSADRESSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde = kilde,
                verdi = ADRESSE_DEFAULT.toJsonNode(),
            ),
        STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.BARN.toJsonNode()),
        FAMILIERELASJON to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                FamilieRelasjon(
                    ansvarligeForeldre = listOf(AVDOED_FOEDSELSNUMMER, GJENLEVENDE_FOEDSELSNUMMER),
                    foreldre = listOf(AVDOED_FOEDSELSNUMMER, GJENLEVENDE_FOEDSELSNUMMER),
                    barn = null,
                ).toJsonNode(),
            ),
    )

internal val halvsoeskenTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> =
    mapOf(
        NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Halv", "mellom", "Søsken").toJsonNode()),
        FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, HALVSOESKEN_FOEDSELSNUMMER.toJsonNode()),
        FOEDSELSDATO to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                HALVSOESKEN_FOEDSELSNUMMER.getBirthDate().toJsonNode(),
            ),
        FOEDSELSAAR to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                HALVSOESKEN_FOEDSELSNUMMER.getBirthDate().year.toJsonNode(),
            ),
        FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        ADRESSEBESKYTTELSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                AdressebeskyttelseGradering.UGRADERT.toJsonNode(),
            ),
        BOSTEDSADRESSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde = kilde,
                verdi = ADRESSE_DEFAULT.toJsonNode(),
            ),
        STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.BARN.toJsonNode()),
        FAMILIERELASJON to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                FamilieRelasjon(
                    ansvarligeForeldre = listOf(AVDOED_FOEDSELSNUMMER, HALVSOESKEN_ANNEN_FORELDER),
                    foreldre = listOf(AVDOED_FOEDSELSNUMMER, HALVSOESKEN_ANNEN_FORELDER),
                    barn = null,
                ).toJsonNode(),
            ),
    )

internal val avdoedTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> =
    mapOf(
        NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Død", "mellom", "Far").toJsonNode()),
        FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, AVDOED_FOEDSELSNUMMER.toJsonNode()),
        FOEDSELSDATO to Opplysning.Konstant(randomUUID(), kilde, AVDOED_FOEDSELSNUMMER.getBirthDate().toJsonNode()),
        FOEDSELSAAR to Opplysning.Konstant(randomUUID(), kilde, AVDOED_FOEDSELSNUMMER.getBirthDate().year.toJsonNode()),
        FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        DOEDSDATO to Opplysning.Konstant(randomUUID(), kilde, LocalDateTime.parse("2022-08-17T00:00:00").toJsonNode()),
        ADRESSEBESKYTTELSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                AdressebeskyttelseGradering.UGRADERT.toJsonNode(),
            ),
        BOSTEDSADRESSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde = kilde,
                verdi = ADRESSE_DEFAULT.toJsonNode(),
            ),
        SIVILSTATUS to Opplysning.Konstant(randomUUID(), kilde, Sivilstatus.GIFT.toJsonNode()),
        STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.AVDOED.toJsonNode()),
        AVDOEDESBARN to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                AvdoedesBarn(
                    listOf(
                        personTestData(soekerTestopplysningerMap),
                        personTestData(soeskenTestopplysningerMap),
                    ),
                ).toJsonNode(),
            ),
        FAMILIERELASJON to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                FamilieRelasjon(
                    ansvarligeForeldre = emptyList(),
                    foreldre = emptyList(),
                    barn = listOf(SOEKER_FOEDSELSNUMMER, HELSOESKEN_FOEDSELSNUMMER),
                ).toJsonNode(),
            ),
    )

val eldreAvdoedTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> =
    avdoedTestopplysningerMap
        .toMutableMap()
        .let { map ->
            map[DOEDSDATO] =
                Opplysning.Konstant(randomUUID(), kilde, AVDOED_FOEDSELSNUMMER.getBirthDate().plusYears(67).toJsonNode())

            map
        }.toMap()

internal val gjenlevendeTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> =
    mapOf(
        NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Levende", "mellom", "Mor").toJsonNode()),
        FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, GJENLEVENDE_FOEDSELSNUMMER.toJsonNode()),
        FOEDSELSDATO to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                GJENLEVENDE_FOEDSELSNUMMER.getBirthDate().toJsonNode(),
            ),
        FOEDSELSAAR to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                GJENLEVENDE_FOEDSELSNUMMER.getBirthDate().year.toJsonNode(),
            ),
        FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        ADRESSEBESKYTTELSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                AdressebeskyttelseGradering.UGRADERT.toJsonNode(),
            ),
        BOSTEDSADRESSE to
            Opplysning.Konstant(
                randomUUID(),
                kilde = kilde,
                verdi = ADRESSE_DEFAULT.toJsonNode(),
            ),
        SIVILSTATUS to Opplysning.Konstant(randomUUID(), kilde, Sivilstatus.GIFT.toJsonNode()),
        STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
        PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.GJENLEVENDE.toJsonNode()),
        FAMILIERELASJON to
            Opplysning.Konstant(
                randomUUID(),
                kilde,
                FamilieRelasjon(
                    ansvarligeForeldre = emptyList(),
                    foreldre = emptyList(),
                    barn = listOf(SOEKER_FOEDSELSNUMMER, HELSOESKEN_FOEDSELSNUMMER),
                ).toJsonNode(),
            ),
    )

fun soeker(): Person =
    Person(
        randomUUID().toString(),
        null,
        randomUUID().toString(),
        SOEKER_FOEDSELSNUMMER,
        null,
        1234,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    )
