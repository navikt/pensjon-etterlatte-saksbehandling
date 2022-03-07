package no.nav.etterlatte.libs.common.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

data class Person(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer,
    val foedselsdato: LocalDate?,
    val foedselsaar: Int,
    val foedeland: String?,
    val doedsdato: LocalDate?,
    val adressebeskyttelse: Adressebeskyttelse,
    var bostedsadresse: List<Adresse>?,
    var deltBostedsadresse: List<Adresse>?,
    var kontaktadresse: List<Adresse>?,
    var oppholdsadresse: List<Adresse>?,
    val sivilstatus: Sivilstatus?,
    val statsborgerskap: String?,
    var utland: Utland?,
    var familieRelasjon: FamilieRelasjon?,
)

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT;
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
    UTENLANDSKADRESSEFRITTFORMAT,
}

data class Adresse(
    val type: AdresseType,
    val aktiv: Boolean,
    val coAdresseNavn: String?,
    val adresseLinje1: String?,
    val adresseLinje2: String?,
    val adresseLinje3: String?,
    val postnr: String?,
    val poststed: String?,
    val land: String?,
    val kilde: String,
    val gyldigFraOgMed: LocalDateTime?,
    val gyldigTilOgMed: LocalDateTime?,
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
    GJENLEVENDE_PARTNER;
}

data class Utland(
    val innflyttingTilNorge: List<InnflyttingTilNorge>?,
    val utflyttingFraNorge: List<UtflyttingFraNorge>?
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
    val ansvarligeForeldre: List<Foedselsnummer>?,
    val foreldre: List<Foedselsnummer>?,
    val barn: List<Foedselsnummer>?
)


fun Person.alder(): Int? {
    return foedselsdato?.let { Period.between(foedselsdato, LocalDate.now()).years }
}

// TODO gir denne egentlig mening i nåværende form? Aktiv tar utgangspunkt i det vi får fra PPS, men det kan
//  i teorien være en adresse som ikke lenger er gyldig da den kan ha satt gyldigTilDato
fun List<Adresse>.aktiv(): Adresse? = firstOrNull { it.aktiv }
