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
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SivilstatusType
import no.nav.etterlatte.libs.common.toJsonNode
import personTestData
import java.time.Instant
import java.time.LocalDateTime

val kilde = Grunnlagsopplysning.Pdl("PDL", Instant.now(), null, "opplysningsId1")

private val AVDØD_FØDSELSNUMMER_DEFAULT = Foedselsnummer.of("01448203510")
private val GJENLEVENDE_FØDSELSNUMMER_DEFAULT = Foedselsnummer.of("29106323621")
private val SØKER_FØDSELSNUMMER_DEFAULT = Foedselsnummer.of("16021254243")
private val SØSKEN_FØDSELSNUMMER_DEFAULT = Foedselsnummer.of("06051250220")
private val ADRESSE_DEFAULT = listOf(
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
        kilde = "PDL",
        gyldigFraOgMed = LocalDateTime.parse("2012-02-16T00:00"),
        gyldigTilOgMed = null
    )
)

internal val søkerTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Interessant", "Träd").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, SØKER_FØDSELSNUMMER_DEFAULT.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, SØKER_FØDSELSNUMMER_DEFAULT.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, SØKER_FØDSELSNUMMER_DEFAULT.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDØD_FØDSELSNUMMER_DEFAULT),
            foreldre = listOf(AVDØD_FØDSELSNUMMER_DEFAULT),
            barn = null
        ).toJsonNode()
    )
)

internal val søskenTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Retorisk", "Mutter").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, SØSKEN_FØDSELSNUMMER_DEFAULT.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, SØSKEN_FØDSELSNUMMER_DEFAULT.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, SØSKEN_FØDSELSNUMMER_DEFAULT.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDØD_FØDSELSNUMMER_DEFAULT),
            foreldre = listOf(AVDØD_FØDSELSNUMMER_DEFAULT),
            barn = null
        ).toJsonNode()
    )
)

internal val avdødTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Nødvendig", "Fusjon").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, AVDØD_FØDSELSNUMMER_DEFAULT.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, AVDØD_FØDSELSNUMMER_DEFAULT.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, AVDØD_FØDSELSNUMMER_DEFAULT.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    DOEDSDATO to Opplysning.Konstant(kilde, LocalDateTime.parse("2022-08-17T00:00:00").toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    SIVILSTATUS to Opplysning.Konstant(kilde, SivilstatusType.EKTESKAP.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.AVDOED.toJsonNode()),
    AVDOEDESBARN to Opplysning.Konstant(
        kilde,
        listOf(personTestData(søkerTestopplysningerMap), personTestData(søskenTestopplysningerMap)).toJsonNode()
    ),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = emptyList(),
            foreldre = emptyList(),
            barn = listOf(SØKER_FØDSELSNUMMER_DEFAULT, SØSKEN_FØDSELSNUMMER_DEFAULT)
        ).toJsonNode()
    )
)

internal val gjenlevendeTestopplysningerMap: Map<Opplysningstyper, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(kilde, Navn("Oppriktig", "Boksamling").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(kilde, GJENLEVENDE_FØDSELSNUMMER_DEFAULT.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(kilde, GJENLEVENDE_FØDSELSNUMMER_DEFAULT.getBirthDate().toJsonNode()),
    FOEDSELSAAR to Opplysning.Konstant(kilde, GJENLEVENDE_FØDSELSNUMMER_DEFAULT.getBirthDate().year.toJsonNode()),
    FOEDELAND to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Konstant(kilde, ADRESSE_DEFAULT.toJsonNode()),
    SIVILSTATUS to Opplysning.Konstant(kilde, SivilstatusType.EKTESKAP.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(kilde, PersonRolle.GJENLEVENDE.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = emptyList(),
            foreldre = emptyList(),
            barn = listOf(SØKER_FØDSELSNUMMER_DEFAULT, SØSKEN_FØDSELSNUMMER_DEFAULT)
        ).toJsonNode()
    )
)