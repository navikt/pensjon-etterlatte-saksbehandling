package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.innsendtsoeknad.OppholdUtlandType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import java.time.LocalDate
import java.time.LocalDateTime

enum class PDLIdentGruppeTyper(val navn: String) {
    FOLKEREGISTERIDENT("FOLKEREGISTERIDENT"), // Kan være DNR og FNR
    AKTORID("AKTORID"),
    NPID("NPID"),
}

data class Person(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val foedselsdato: LocalDate?,
    val foedselsaar: Int,
    val foedeland: String?,
    val doedsdato: LocalDate?,
    val adressebeskyttelse: AdressebeskyttelseGradering?,
    var bostedsadresse: List<Adresse>?,
    var deltBostedsadresse: List<Adresse>?,
    var kontaktadresse: List<Adresse>?,
    var oppholdsadresse: List<Adresse>?,
    val sivilstatus: Sivilstatus?,
    val sivilstand: List<Sivilstand>?,
    val statsborgerskap: String?,
    val pdlStatsborgerskap: List<Statsborgerskap>?,
    var utland: Utland?,
    var familieRelasjon: FamilieRelasjon?,
    var avdoedesBarn: List<Person>?,
    var avdoedesBarnUtenIdent: List<PersonUtenIdent>?,
    var vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>?,
)

data class Statsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
)

enum class AdresseType {
    VEGADRESSE,
    MATRIKKELADRESSE,
    UTENLANDSKADRESSE,
    OPPHOLD_ANNET_STED,
    UKJENT_BOSTED,
    UKJENT,
    POSTBOKSADRESSE,
    POSTADRESSEFRITTFORMAT,
    UTENLANDSKADRESSEFRITTFORMAT,
}

data class Adresse(
    val type: AdresseType,
    val aktiv: Boolean,
    val coAdresseNavn: String? = null,
    val adresseLinje1: String? = null,
    val adresseLinje2: String? = null,
    val adresseLinje3: String? = null,
    val postnr: String? = null,
    val poststed: String? = null,
    val land: String? = null,
    val kilde: String,
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
)

enum class Sivilstatus {
    UOPPGITT,
    UGIFT,
    GIFT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER,
}

data class Sivilstand(
    val sivilstatus: Sivilstatus,
    val relatertVedSiviltilstand: Folkeregisteridentifikator?,
    val gyldigFraOgMed: LocalDate?,
    val bekreftelsesdato: LocalDate?,
    val kilde: String,
)

data class Utland(
    val innflyttingTilNorge: List<InnflyttingTilNorge>?,
    val utflyttingFraNorge: List<UtflyttingFraNorge>?,
)

data class InnflyttingTilNorge(
    val fraflyttingsland: String?,
    val dato: LocalDate?,
)

data class UtflyttingFraNorge(
    val tilflyttingsland: String?,
    val dato: LocalDate?,
)

data class FamilieRelasjon(
    val ansvarligeForeldre: List<Folkeregisteridentifikator>?,
    val foreldre: List<Folkeregisteridentifikator>?,
    val barn: List<Folkeregisteridentifikator>?,
    val personerUtenIdent: List<PersonUtenIdent>? = null,
)

data class AvdoedesBarn(
    @JsonValue
    val avdoedesBarn: List<Person>?,
) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun create(barn: List<Person>?) = AvdoedesBarn(barn)
    }
}

data class VergemaalEllerFremtidsfullmakt(
    val embete: String?,
    val type: String?,
    val vergeEllerFullmektig: VergeEllerFullmektig,
)

data class VergeEllerFullmektig(
    val motpartsPersonident: Folkeregisteridentifikator?,
    val navn: String?,
    val omfang: String?,
    val omfangetErInnenPersonligOmraade: Boolean?,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class PdlIdentifikator {
    @JsonTypeName("FOLKEREGISTERIDENT")
    data class FolkeregisterIdent(
        val folkeregisterident: Folkeregisteridentifikator,
    ) : PdlIdentifikator()

    @JsonTypeName("NPID")
    data class Npid(
        val npid: NavPersonIdent,
    ) : PdlIdentifikator()
}

data class Utenlandsadresse(
    val harHattUtenlandsopphold: JaNeiVetIkke,
    val land: String?,
    val adresse: String?,
)

data class UtenlandsoppholdOpplysninger(
    val harHattUtenlandsopphold: JaNeiVetIkke,
    val land: String,
    val oppholdsType: List<OppholdUtlandType>,
    val medlemFolketrygd: JaNeiVetIkke,
    val pensjonsutbetaling: String?,
)

data class GeografiskTilknytning(
    val kommune: String? = null,
    val bydel: String? = null,
    val land: String? = null,
    val ukjent: Boolean = false,
) {
    fun geografiskTilknytning() =
        when {
            bydel != null -> bydel
            kommune != null -> kommune
            land != null -> land
            else -> null
        }
}

interface Verge {
    fun navn(): String
}

data class Vergemaal(val mottaker: BrevMottaker) : Verge {
    override fun navn(): String {
        return mottaker.navn!!
    }
}

data class ForelderVerge(val navn: String) : Verge {
    override fun navn(): String {
        return navn
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrevMottaker(
    val navn: String?,
    val foedselsnummer: MottakerFoedselsnummer?,
    val adresse: MottakerAdresse,
)

/**
 * Denne pakker inn fødselsnummer i {value: "<fnr>"}, fordi det skal matche responsen fra brev-api...
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MottakerFoedselsnummer(val value: String) {
    /**
     * Skal ALLTID returnere anonymisert fødselsnummer.
     *
     * Bruk [value] ved behov for komplett fødselsnummer.
     */
    override fun toString(): String = this.value.replaceRange(6 until 11, "*****")
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MottakerAdresse(
    val adresseType: String,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String,
    val land: String,
)

// TODO gir denne egentlig mening i nåværende form? Aktiv tar utgangspunkt i det vi får fra PPS, men det kan
//  i teorien være en adresse som ikke lenger er gyldig da den kan ha satt gyldigTilDato
fun List<Adresse>.aktiv(): Adresse? = firstOrNull { it.aktiv }

fun List<Adresse>.nyeste(inkluderInaktiv: Boolean = false): Adresse? =
    sortedByDescending { it.gyldigFraOgMed }.firstOrNull { if (inkluderInaktiv) true else it.gyldigTilOgMed == null }

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
    ;

    fun erGradert(): Boolean {
        return this == STRENGT_FORTROLIG_UTLAND || this == STRENGT_FORTROLIG || this == FORTROLIG
    }
}

fun List<AdressebeskyttelseGradering?>.hentPrioritertGradering() = this.filterNotNull().minOrNull() ?: AdressebeskyttelseGradering.UGRADERT

fun hentRelevantVerge(vergeListe: List<VergemaalEllerFremtidsfullmakt>?): VergemaalEllerFremtidsfullmakt? {
    return vergeListe?.firstOrNull {
        it.vergeEllerFullmektig.omfang in alleVergeOmfangMedOekonomiskeInteresser
    }
}

fun flereVergerMedOekonomiskInteresse(vergeListe: List<VergemaalEllerFremtidsfullmakt>?): Boolean {
    val verger =
        vergeListe?.filter {
            it.vergeEllerFullmektig.omfang in alleVergeOmfangMedOekonomiskeInteresser
        } ?: emptyList()
    return verger.size > 1
}

fun finnesVergeMedUkjentOmfang(vergeListe: List<VergemaalEllerFremtidsfullmakt>?): Boolean {
    val verger =
        vergeListe?.filter {
            it.vergeEllerFullmektig.omfang !in alleKjenteVergeOmfang
        } ?: emptyList()
    return verger.size > 1
}

private val alleVergeOmfangMedOekonomiskeInteresser =
    listOf(
        "utlendingssakerPersonligeOgOekonomiskeInteresser",
        "personligeOgOekonomiskeInteresser",
        "oekonomiskeInteresser",
    )

private val alleKjenteVergeOmfang =
    alleVergeOmfangMedOekonomiskeInteresser + listOf("personligeInteresser")
