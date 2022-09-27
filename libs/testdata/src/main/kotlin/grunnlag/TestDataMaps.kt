package grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.ADRESSEBESKYTTELSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.DOEDSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FAMILIERELASJON
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSAAR
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.PERSONROLLE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.SIVILSTATUS
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.toJsonNode
import personTestData
import java.time.Instant
import java.time.LocalDateTime

val kilde = Grunnlagsopplysning.Pdl("pdl", Instant.now(), null, "opplysningsId1")

val AVDØD_FØDSELSNUMMER = Foedselsnummer.of("01448203510")
val GJENLEVENDE_FØDSELSNUMMER = Foedselsnummer.of("29106323621")
val SØKER_FØDSELSNUMMER = Foedselsnummer.of("16021254243")
val HELSØSKEN_FØDSELSNUMMER = Foedselsnummer.of("06051250220")
val HALVSØSKEN_FØDSELSNUMMER = Foedselsnummer.of("09011076618")
val HALVSØSKEN_ANNEN_FORELDER = Foedselsnummer.of("20127905941")

val ADRESSE_DEFAULT =
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
        gyldigTilOgMed = null
    )

internal val søkerTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Søker", "Barn").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, SØKER_FØDSELSNUMMER.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, SØKER_FØDSELSNUMMER.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, SØKER_FØDSELSNUMMER.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDØD_FØDSELSNUMMER, GJENLEVENDE_FØDSELSNUMMER),
            foreldre = listOf(AVDØD_FØDSELSNUMMER, GJENLEVENDE_FØDSELSNUMMER),
            barn = null
        ).toJsonNode()
    )
)

internal val søskenTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Hel", "Søsken").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, HELSØSKEN_FØDSELSNUMMER.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, HELSØSKEN_FØDSELSNUMMER.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, HELSØSKEN_FØDSELSNUMMER.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDØD_FØDSELSNUMMER, GJENLEVENDE_FØDSELSNUMMER),
            foreldre = listOf(AVDØD_FØDSELSNUMMER, GJENLEVENDE_FØDSELSNUMMER),
            barn = null
        ).toJsonNode()
    )
)

internal val halvsøskenTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Halv", "Søsken").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, HALVSØSKEN_FØDSELSNUMMER.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, HALVSØSKEN_FØDSELSNUMMER.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, HALVSØSKEN_FØDSELSNUMMER.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDØD_FØDSELSNUMMER, HALVSØSKEN_ANNEN_FORELDER),
            foreldre = listOf(AVDØD_FØDSELSNUMMER, HALVSØSKEN_ANNEN_FORELDER),
            barn = null
        ).toJsonNode()
    )
)

internal val avdødTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Død", "Far").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, AVDØD_FØDSELSNUMMER.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, AVDØD_FØDSELSNUMMER.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, AVDØD_FØDSELSNUMMER.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    DOEDSDATO to Opplysning.Konstant(kilde, LocalDateTime.parse("2022-08-17T00:00:00").toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    SIVILSTATUS to Opplysning.Konstant(kilde, Sivilstatus.GIFT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.AVDOED.toJsonNode()),
    AVDOEDESBARN to Opplysning.Konstant(
        kilde,
        AvdoedesBarn(listOf(personTestData(søkerTestopplysningerMap), personTestData(søskenTestopplysningerMap)))
            .toJsonNode()
    ),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = emptyList(),
            foreldre = emptyList(),
            barn = listOf(SØKER_FØDSELSNUMMER, HELSØSKEN_FØDSELSNUMMER)
        ).toJsonNode()
    )
)

internal val gjenlevendeTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Levende", "Mor").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, GJENLEVENDE_FØDSELSNUMMER.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, GJENLEVENDE_FØDSELSNUMMER.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, GJENLEVENDE_FØDSELSNUMMER.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    SIVILSTATUS to Opplysning.Konstant(kilde, Sivilstatus.GIFT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.GJENLEVENDE.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = emptyList(),
            foreldre = emptyList(),
            barn = listOf(SØKER_FØDSELSNUMMER, HELSØSKEN_FØDSELSNUMMER)
        ).toJsonNode()
    )
)