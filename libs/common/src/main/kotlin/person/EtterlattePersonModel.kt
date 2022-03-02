package no.nav.etterlatte.libs.common.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

//TODO vurdere å endre til logiske personer? barn/forelder/avdoed/etterlatt
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
}

data class Adresse(
    val type: AdresseType,
    val aktiv: Boolean,
    val adresseLinje1: String?,
    val adresseLinje2: String?,
    val postnr: String?,
    val poststed: String?,
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
    val ansvarligeForeldre: List<ForeldreAnsvar>?,
    val foreldre: List<Foreldre>?,
    val barn: List<Barn>?
)

data class ForeldreAnsvar(val foedselsnummer: Foedselsnummer)

data class Foreldre(val foedselsnummer: Foedselsnummer)

data class Barn (val foedselsnummer: Foedselsnummer)

fun Person.alder(): Int? {
    return foedselsdato?.let { Period.between(foedselsdato, LocalDate.now()).years }
}

fun List<Adresse>.aktiv(): Adresse? = firstOrNull { it.aktiv }
