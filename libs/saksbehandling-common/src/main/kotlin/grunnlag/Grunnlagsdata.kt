package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.ADRESSEBESKYTTELSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.DELTBOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.DOEDSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FAMILIERELASJON
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSAAR
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.KONTAKTADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.OPPHOLDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONROLLE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SIVILSTAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SIVILSTATUS
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTENLANDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTENLANDSOPPHOLD
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTLAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Utenlandsadresse
import no.nav.etterlatte.libs.common.person.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.toJson
import java.time.LocalDate

typealias Grunnlagsdata<T> = Map<Opplysningstype, Opplysning<T>>

fun Grunnlagsdata<JsonNode>.hentNavn() = this.hentKonstantOpplysning<Navn>(NAVN)
fun Grunnlagsdata<JsonNode>.hentFoedselsnummer() = this.hentKonstantOpplysning<Folkeregisteridentifikator>(
    FOEDSELSNUMMER
)
fun Grunnlagsdata<JsonNode>.hentFoedselsdato() = this.hentKonstantOpplysning<LocalDate>(FOEDSELSDATO)
fun Grunnlagsdata<JsonNode>.hentFoedselsaar() = this.hentKonstantOpplysning<Int>(FOEDSELSAAR)
fun Grunnlagsdata<JsonNode>.hentFoedeland() = this.hentKonstantOpplysning<String>(FOEDELAND)
fun Grunnlagsdata<JsonNode>.hentDoedsdato() = this.hentKonstantOpplysning<LocalDate?>(DOEDSDATO)
fun Grunnlagsdata<JsonNode>.hentAdressebeskyttelse() =
    this.hentKonstantOpplysning<AdressebeskyttelseGradering>(ADRESSEBESKYTTELSE)

fun Grunnlagsdata<JsonNode>.hentBostedsadresse() = this.hentKonstantOpplysning<List<Adresse>>(BOSTEDSADRESSE)
fun Grunnlagsdata<JsonNode>.hentDeltbostedsadresse() = this.hentKonstantOpplysning<List<Adresse>>(DELTBOSTEDSADRESSE)
fun Grunnlagsdata<JsonNode>.hentKontaktadresse() = this.hentKonstantOpplysning<List<Adresse>>(KONTAKTADRESSE)
fun Grunnlagsdata<JsonNode>.hentOppholdsadresse() = this.hentKonstantOpplysning<List<Adresse>>(OPPHOLDSADRESSE)
fun Grunnlagsdata<JsonNode>.hentSivilstatus() = this.hentKonstantOpplysning<Sivilstatus>(SIVILSTATUS)
fun Grunnlagsdata<JsonNode>.hentSivilstand() = this.hentKonstantOpplysning<List<Sivilstand>>(SIVILSTAND)
fun Grunnlagsdata<JsonNode>.hentStatsborgerskap() = this.hentKonstantOpplysning<String>(STATSBORGERSKAP)
fun Grunnlagsdata<JsonNode>.hentUtland() = this.hentKonstantOpplysning<Utland>(UTLAND)
fun Grunnlagsdata<JsonNode>.hentFamilierelasjon() = this.hentKonstantOpplysning<FamilieRelasjon>(FAMILIERELASJON)
fun Grunnlagsdata<JsonNode>.hentAvdoedesbarn() = this.hentKonstantOpplysning<AvdoedesBarn>(AVDOEDESBARN)
fun Grunnlagsdata<JsonNode>.hentVergemaalellerfremtidsfullmakt() =
    this.hentKonstantOpplysning<List<VergemaalEllerFremtidsfullmakt>>(VERGEMAALELLERFREMTIDSFULLMAKT)

fun Grunnlagsdata<JsonNode>.hentPersonrolle() = this.hentKonstantOpplysning<PersonRolle>(PERSONROLLE)
fun Grunnlagsdata<JsonNode>.hentUtenlandsopphold() =
    this.hentKonstantOpplysning<UtenlandsoppholdOpplysninger>(UTENLANDSOPPHOLD)

fun Grunnlagsdata<JsonNode>.hentUtenlandsadresse() =
    this.hentKonstantOpplysning<Utenlandsadresse>(UTENLANDSADRESSE)

inline fun <reified T> Grunnlagsdata<JsonNode>.hentKonstantOpplysning(
    opplysningstype: Opplysningstype
): Opplysning.Konstant<T>? {
    val grunnlagsdata = this[opplysningstype] ?: return null

    return when (grunnlagsdata) {
        is Opplysning.Konstant -> Opplysning.Konstant(
            grunnlagsdata.id,
            grunnlagsdata.kilde,
            objectMapper.readValue(grunnlagsdata.verdi.toJson(), object : TypeReference<T>() {})
        )
    }
}