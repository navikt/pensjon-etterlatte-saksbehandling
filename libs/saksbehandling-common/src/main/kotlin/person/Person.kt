package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.innsendtsoeknad.OppholdUtlandType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.logging.sikkerlogger
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
    val tjenesteomraade: String?,
    val omfangetErInnenPersonligOmraade: Boolean?,
    val omfang: String? = null,
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
    fun navn(): String?
}

data class Vergemaal(val mottaker: BrevMottaker) : Verge {
    override fun navn(): String? {
        return mottaker.navn
    }
}

data class ForelderVerge(val foedselsnummer: Folkeregisteridentifikator, val navn: String) : Verge {
    override fun navn(): String {
        return navn
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrevMottaker(
    val navn: String?,
    val foedselsnummer: MottakerFoedselsnummer?,
    val adresse: MottakerAdresse?,
    val adresseTypeIKilde: String? = null,
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

    fun erStrengtFortrolig(): Boolean {
        return this == STRENGT_FORTROLIG_UTLAND || this == STRENGT_FORTROLIG
    }
}

fun finnHoyestGradering(graderinger: List<AdressebeskyttelseGradering>): AdressebeskyttelseGradering {
    val strengtFortroligGradering =
        graderinger.find {
            it in
                listOf(
                    AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                    AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
                )
        }

    if (strengtFortroligGradering != null) {
        return strengtFortroligGradering
    }

    val fortrolig = graderinger.find { it == AdressebeskyttelseGradering.FORTROLIG }

    if (fortrolig != null) {
        return fortrolig
    }

    return AdressebeskyttelseGradering.UGRADERT
}

fun finnHoyesteGradering(
    graderingEn: AdressebeskyttelseGradering,
    graderingTo: AdressebeskyttelseGradering,
): AdressebeskyttelseGradering {
    if (graderingEn in listOf(AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND)) {
        return graderingEn
    }

    if (graderingTo in listOf(AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND)) {
        return graderingTo
    }

    if (graderingEn == AdressebeskyttelseGradering.FORTROLIG) {
        return graderingEn
    }

    if (graderingTo == AdressebeskyttelseGradering.FORTROLIG) {
        return graderingTo
    }

    return graderingEn
}

fun List<AdressebeskyttelseGradering?>.hentPrioritertGradering() = this.filterNotNull().minOrNull() ?: AdressebeskyttelseGradering.UGRADERT

fun hentRelevantVerge(
    vergeListe: List<VergemaalEllerFremtidsfullmakt>?,
    soekersFnr: Folkeregisteridentifikator?,
): VergemaalEllerFremtidsfullmakt? {
    val oekonomisk =
        vergeListe?.firstOrNull { vergemaal ->
            vergemaal.vergeEllerFullmektig.tjenesteomraade in alleVergeOmfangMedOekonomiskeInteresser &&
                harVergensFnr(vergemaal, soekersFnr)
        }

    return oekonomisk ?: vergeListe?.firstOrNull { vergemaal ->
        harVergensFnr(vergemaal, soekersFnr)
    }
}

private fun harVergensFnr(
    vergemaal: VergemaalEllerFremtidsfullmakt,
    soekersFnr: Folkeregisteridentifikator?,
): Boolean {
    val manglerFnr = vergemaal.vergeEllerFullmektig.motpartsPersonident == null
    if (manglerFnr) {
        sikkerlogger()
            .error(
                "Et vergemål for person '${soekersFnr?.value}' i PDL mangler vergens fødselsnummer. " +
                    "Vergens navn: ${vergemaal.vergeEllerFullmektig.navn}.",
            )
    }
    return !manglerFnr
}

private val alleVergeOmfangMedOekonomiskeInteresser =
    listOf(
        "utlendingssakerPersonligeOgOekonomiskeInteresser",
        "personligeOgOekonomiskeInteresser",
        "oekonomiskeInteresser",
    )
