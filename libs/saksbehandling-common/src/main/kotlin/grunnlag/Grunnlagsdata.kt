package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
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
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOESKEN_I_BEREGNINGEN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTENLANDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTENLANDSOPPHOLD
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.UTLAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Adresse
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
import org.slf4j.LoggerFactory.getLogger
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

fun Grunnlagsdata<JsonNode>.hentBostedsadresse() = this.hentPeriodisertOpplysning<Adresse>(BOSTEDSADRESSE)
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
    this.hentPeriodisertOpplysning<UtenlandsoppholdOpplysninger>(UTENLANDSOPPHOLD)

fun Grunnlagsdata<JsonNode>.hentUtenlandsadresse() =
    this.hentKonstantOpplysning<Utenlandsadresse>(UTENLANDSADRESSE)

fun Grunnlagsdata<JsonNode>.hentSoeskenjustering() =
    this.hentKonstantOpplysning<Beregningsgrunnlag>(SOESKEN_I_BEREGNINGEN)

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

        else -> {
            getLogger(this::class.java).error("Feil skjedde under henting av opplysning: Opplysningen er periodisert")
            throw RuntimeException("Feil skjedde under henting av opplysning: Opplysningen er periodisert")
        }
    }
}

inline fun <reified T> Grunnlagsdata<JsonNode>.hentPeriodisertOpplysning(
    opplysningstype: Opplysningstype
): Opplysning.Periodisert<T>? {
    val grunnlagsdata = this[opplysningstype] ?: return null

    return when (grunnlagsdata) {
        is Opplysning.Periodisert -> {
            Opplysning.Periodisert(
                perioder = grunnlagsdata.perioder.map {
                    PeriodisertOpplysning(
                        id = it.id,
                        kilde = it.kilde,
                        verdi = objectMapper.readValue(it.verdi.toJson(), T::class.java),
                        fom = it.fom,
                        tom = it.tom
                    )
                }
            )
        }

        else -> {
            val err = "Feil skjedde under henting av opplysning $opplysningstype: Opplysningen er Konstant"
            getLogger(this::class.java).error(err)
            throw RuntimeException(err)
        }
    }
}