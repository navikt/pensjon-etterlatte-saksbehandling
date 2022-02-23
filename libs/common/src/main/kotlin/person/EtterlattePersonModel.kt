package no.nav.etterlatte.libs.common.person

import java.time.LocalDate
import java.time.LocalDateTime

//TODO endre til logiske personer? barn/forelder/avdoed/etterlatt
data class Person(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer,
    val foedselsaar: Int?,
    val foedselsdato: String?,
    val doedsdato: String?,
    val adressebeskyttelse: Adressebeskyttelse,
    var adresse: Adresse?,
    val statsborgerskap: String?,
    val foedeland: String?,
    val sivilstatus: String?,
    var utland: Utland?,
    var familieRelasjon: FamilieRelasjon?
)

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
    val dato: String?,

)

data class UtflyttingFraNorge(
    val tilflyttingsland: String?,
    val dato: String?,
)

enum class Rolle {
    BARN,
    AVDOED,
    ETTERLATT;
}

fun Person.alder(): Int {
    var alder = LocalDateTime.now().year - foedselsaar!!
    if (LocalDateTime.now().dayOfYear >= LocalDate.parse(foedselsdato).dayOfYear) alder++
    return alder
}

//TODO diskutere med FAG, hva trenger vi egentlig fra 'Adresse'?
data class Adresse(
    val bostedsadresse: Bostedsadresse?,
    val kontaktadresse: Kontaktadresse?,
    val oppholdsadresse: Oppholdsadresse?
//TODO tenke på noe som gjør det lettere for resten å finne rett adresse
//String representasjon med adresselinjer?
)

fun aktivadresse(): String{
    return "BostedsAdresse"
}
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