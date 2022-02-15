package no.nav.etterlatte.libs.common.person

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Barn
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
    val adressebeskyttelse: Boolean,
    var adresse: eyAdresse?,
    val statsborgerskap: String?,
    val foedeland: String?,
    val sivilstatus: String?,
    var utland: eyUtland?,
    var familieRelasjon: EyFamilieRelasjon?,
    val rolle: Rolle?
)


data class eyUtland(
    val innflyttingTilNorge: List<eyInnflyttingTilNorge>?,
    val utflyttingFraNorge: List<eyUtflyttingFraNorge>?
)
data class eyInnflyttingTilNorge(
    val fraflyttingsland: String?,
    val dato: String?,

)

data class eyUtflyttingFraNorge(
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
data class eyAdresse(
val bostedsadresse: EyBostedsadresse?,
val kontaktadresse: EyKontaktadresse?,
val oppholdsadresse: EyOppholdsadresse?
//TODO tenke på noe som gjør det lettere for resten å finne rett adresse
//String representasjon med adresselinjer?
)fun aktivadresse(): String{
    return "BostedsAdresse"
}
data class EyBostedsadresse(
    val vegadresse: EyVegadresse
)
data class EyKontaktadresse(
    val vegadresse: EyVegadresse
)
data class EyOppholdsadresse(
    val vegadresse: EyVegadresse
)
data class EyVegadresse(
    val adressenavn: String?,
    val husnummer: String?,
    val husbokstav: String?,
    val postnummer: String?
)

//TODO hva kaller vi noen med foreldreansvar
data class EyFamilieRelasjon(
    val ansvarligeForeldre: List<EyForeldreAnsvar>?,
    val foreldre: List<EyForeldre>?,
    val barn: List<EyBarn>?
)

data class EyForeldreAnsvar(val foedselsnummer: Foedselsnummer)
data class EyForeldre(val foedselsnummer: Foedselsnummer)
data class EyBarn (val foedselsnummer: Foedselsnummer)