package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.innsendtsoeknad.OppholdUtlandType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import java.time.LocalDate
import java.time.LocalDateTime

enum class PDLIdentGruppeTyper(
    val navn: String,
) {
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

    // Uavklart brukes når PPS ikke kan avklare mellom flere forskjellige sivilstatuser,
    // og er ikke en del av datamodellen til PDL
    UAVKLART_PPS,
}

data class Sivilstand(
    val sivilstatus: Sivilstatus,
    val relatertVedSiviltilstand: Folkeregisteridentifikator?,
    val gyldigFraOgMed: LocalDate?,
    val bekreftelsesdato: LocalDate?,
    // Alle sivilstander hentet frem til nå har vært aktive
    // Setter default til false for å støtte sivilstander uten dette feltet frem til en migrering er gjennomført
    val historisk: Boolean = false,
    val kilde: String,
)

data class Utland(
    val innflyttingTilNorge: List<InnflyttingTilNorge>?,
    val utflyttingFraNorge: List<UtflyttingFraNorge>?,
)

data class InnflyttingTilNorge(
    val fraflyttingsland: String?,
    val dato: LocalDate?,
    val gyldighetsdato: LocalDate?,
    val ajourholdsdato: LocalDate?,
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
    val opphoerstidspunkt: LocalDateTime?,
)

data class VergeEllerFullmektig(
    val motpartsPersonident: Folkeregisteridentifikator?,
    val navn: String?,
    val tjenesteomraade: String?,
    val omfangetErInnenPersonligOmraade: Boolean?,
    val omfang: String? = null,
)

data class PdlFolkeregisterIdentListe(
    val identifikatorer: List<PdlIdentifikator.FolkeregisterIdent>,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class PdlIdentifikator(
    open val historisk: Boolean,
) {
    @JsonTypeName("FOLKEREGISTERIDENT")
    data class FolkeregisterIdent(
        val folkeregisterident: Folkeregisteridentifikator,
        override val historisk: Boolean = false,
    ) : PdlIdentifikator(historisk)

    @JsonTypeName("NPID")
    data class Npid(
        val npid: NavPersonIdent,
        override val historisk: Boolean = false,
    ) : PdlIdentifikator(historisk)

    @JsonTypeName("AKTORID")
    data class AktoerId(
        val aktoerId: String,
    )
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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Vergemaal::class, name = "Vergemaal"),
    JsonSubTypes.Type(value = UkjentVergemaal::class, name = "UkjentVergemaal"),
    JsonSubTypes.Type(value = ForelderVerge::class, name = "ForelderVerge"),
)
interface Verge {
    fun navn(): String?

    val type: String
}

data class Vergemaal(
    val navn: String,
    val foedselsnummer: Folkeregisteridentifikator,
) : Verge {
    override val type: String = "Vergemaal"

    override fun navn(): String = navn
}

class UkjentVergemaal : Verge {
    override val type: String = "UkjentVergemaal"

    override fun navn(): String? = null
}

data class ForelderVerge(
    val foedselsnummer: Folkeregisteridentifikator,
    val navn: String,
) : Verge {
    override val type: String = "ForelderVerge"

    override fun navn(): String = navn
}

/**
 * Denne pakker inn fødselsnummer i {value: "<fnr>"}, fordi det skal matche responsen fra brev-api...
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MottakerFoedselsnummer(
    val value: String,
) {
    /**
     * Skal ALLTID returnere anonymisert fødselsnummer.
     *
     * Bruk [value] ved behov for komplett fødselsnummer.
     */
    override fun toString(): String = this.value.replaceRange(6 until 11, "*****")
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
    ;

    fun harAdressebeskyttelse(): Boolean = this == STRENGT_FORTROLIG_UTLAND || this == STRENGT_FORTROLIG || this == FORTROLIG
}

fun List<AdressebeskyttelseGradering?>.hentPrioritertGradering() = this.filterNotNull().minOrNull() ?: AdressebeskyttelseGradering.UGRADERT

fun hentVerger(
    vergeListe: List<VergemaalEllerFremtidsfullmakt>,
    soekersFnr: Folkeregisteridentifikator?,
): List<VergemaalEllerFremtidsfullmakt> =
    vergeListe.filter { vergemaal ->
        harVergensFnr(vergemaal, soekersFnr)
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
