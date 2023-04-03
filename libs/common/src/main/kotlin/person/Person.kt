package no.nav.etterlatte.libs.common.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.innsendtsoeknad.OppholdUtlandType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

enum class PDLIdentGruppeTyper(val navn: String) {
    FOLKEREGISTERIDENT("FOLKEREGISTERIDENT"), // Kan være DNR og FNR
    AKTORID("AKTORID"),
    NPID("NPID")
}

data class Person(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val foedselsdato: LocalDate?,
    val foedselsaar: Int,
    val foedeland: String?,
    val doedsdato: LocalDate?,
    val adressebeskyttelse: Adressebeskyttelse?,
    var bostedsadresse: List<Adresse>?,
    var deltBostedsadresse: List<Adresse>?,
    var kontaktadresse: List<Adresse>?,
    var oppholdsadresse: List<Adresse>?,
    val sivilstatus: Sivilstatus?,
    val sivilstand: List<Sivilstand>?,
    val statsborgerskap: String?,
    var utland: Utland?,
    var familieRelasjon: FamilieRelasjon?,
    var avdoedesBarn: List<Person>?,
    var vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>?
)

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

enum class AdresseType {
    VEGADRESSE,
    MATRIKKELADRESSE,
    UTENLANDSKADRESSE,
    OPPHOLD_ANNET_STED,
    UKJENT_BOSTED,
    UKJENT,
    POSTBOKSADRESSE,
    POSTADRESSEFRITTFORMAT,
    UTENLANDSKADRESSEFRITTFORMAT
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
    val gyldigTilOgMed: LocalDateTime? = null
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
    GJENLEVENDE_PARTNER
}

data class Sivilstand(
    val sivilstatus: Sivilstatus,
    val relatertVedSiviltilstand: Folkeregisteridentifikator?,
    val gyldigFraOgMed: LocalDate?,
    val bekreftelsesdato: LocalDate?,
    val kilde: String
)

data class Utland(
    val innflyttingTilNorge: List<InnflyttingTilNorge>?,
    val utflyttingFraNorge: List<UtflyttingFraNorge>?
)

data class InnflyttingTilNorge(
    val fraflyttingsland: String?,
    val dato: LocalDate?

)

data class UtflyttingFraNorge(
    val tilflyttingsland: String?,
    val dato: LocalDate?
)

data class FamilieRelasjon(
    val ansvarligeForeldre: List<Folkeregisteridentifikator>?,
    val foreldre: List<Folkeregisteridentifikator>?,
    val barn: List<Folkeregisteridentifikator>?
)

data class AvdoedesBarn(
    @JsonValue
    val avdoedesBarn: List<Person>?
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
    val vergeEllerFullmektig: VergeEllerFullmektig
)

data class VergeEllerFullmektig(
    val motpartsPersonident: Folkeregisteridentifikator?,
    val navn: String?,
    val omfang: String?,
    val omfangetErInnenPersonligOmraade: Boolean
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class PdlIdentifikator {

    @JsonTypeName("FOLKEREGISTERIDENT")
    data class FolkeregisterIdent(
        val folkeregisterident: Folkeregisteridentifikator
    ) : PdlIdentifikator()

    @JsonTypeName("NPID")
    data class Npid(
        val npid: NavPersonIdent
    ) : PdlIdentifikator()
}

data class Utenlandsadresse(
    val harHattUtenlandsopphold: JaNeiVetIkke,
    val land: String?,
    val adresse: String?
)

data class UtenlandsoppholdOpplysninger(
    val harHattUtenlandsopphold: JaNeiVetIkke,
    val land: String,
    val oppholdsType: List<OppholdUtlandType>,
    val medlemFolketrygd: JaNeiVetIkke,
    val pensjonsutbetaling: String?
)

data class GeografiskTilknytning(
    val kommune: String? = null,
    val bydel: String? = null,
    val land: String? = null,
    val ukjent: Boolean = false
) {
    fun geografiskTilknytning() = when {
        bydel != null -> bydel
        kommune != null -> kommune
        land != null -> land
        else -> null
    }
}

fun Person.alder(): Int? {
    return foedselsdato?.let { Period.between(foedselsdato, LocalDate.now()).years }
}

// TODO gir denne egentlig mening i nåværende form? Aktiv tar utgangspunkt i det vi får fra PPS, men det kan
//  i teorien være en adresse som ikke lenger er gyldig da den kan ha satt gyldigTilDato
fun List<Adresse>.aktiv(): Adresse? = firstOrNull { it.aktiv }

fun List<Adresse>.nyeste(inkluderInaktiv: Boolean = false): Adresse? =
    sortedByDescending { it.gyldigFraOgMed }.firstOrNull { if (inkluderInaktiv) true else it.gyldigTilOgMed == null }

class FamilieRelasjonManglerIdent(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)