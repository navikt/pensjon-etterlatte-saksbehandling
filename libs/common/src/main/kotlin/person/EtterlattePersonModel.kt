package no.nav.etterlatte.libs.common.person

import java.time.LocalDate
import java.time.LocalDateTime

//TODO endre til logiske personer? barn/forelder/avdoed/etterlatt
data class Person(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer,
    val foedselsdato: LocalDate?,
    val foedselsaar: Int,
    val foedeland: String?,
    val doedsdato: LocalDate?,
    val adressebeskyttelse: Adressebeskyttelse,
    var bostedsadresse: List<Adresse2>?,
    var deltBostedsadresse: List<Adresse2>?,
    var kontaktadresse: List<Adresse2>?,
    var oppholdsadresse: List<Adresse2>?,
    val sivilstatus: Sivilstatus?,
    val statsborgerskap: String?,
    var utland: Utland?,
    var familieRelasjon: FamilieRelasjon?,

    @Deprecated("Skal fjernes")
    var adresse: Adresse?,
)

fun List<Adresse2>.aktiv(): Adresse2? = firstOrNull { it.aktiv }

enum class Adressebeskyttelse {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT;
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

enum class Rolle {
    BARN,
    AVDOED,
    ETTERLATT;
}

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

// TODO - denne bør fikses
fun Person.alder(): Int {
    var alder = LocalDateTime.now().year - foedselsaar
    if (LocalDateTime.now().dayOfYear >= foedselsdato?.dayOfYear!!) alder++
    return alder
}

data class Adresse(
    val bostedsadresse: Bostedsadresse?,
    val kontaktadresse: Kontaktadresse?,
    val oppholdsadresse: Oppholdsadresse?
)

enum class AdresseType {
    VEGADRESSE,
    MATRIKKELADRESSE,
    UTENLANDSKADRESSE,
    OPPHOLD_ANNET_STED,
    UKJENT_BOSTED,
    UKJENT,
}

data class Adresse2(
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

data class Bostedsadresse(
    val vegadresse: Vegadresse
)

data class Kontaktadresse(
    val vegadresse: Vegadresse
)

data class Oppholdsadresse(
    val vegadresse: Vegadresse
)

data class Vegadresse(
    val adressenavn: String?,
    val husnummer: String?,
    val husbokstav: String?,
    val postnummer: String?
)

//TODO hva kaller vi noen med foreldreansvar
data class FamilieRelasjon(
    val ansvarligeForeldre: List<ForeldreAnsvar>?,
    val foreldre: List<Foreldre>?,
    val barn: List<Barn>?
)

data class ForeldreAnsvar(val foedselsnummer: Foedselsnummer)

data class Foreldre(val foedselsnummer: Foedselsnummer)

data class Barn (val foedselsnummer: Foedselsnummer)