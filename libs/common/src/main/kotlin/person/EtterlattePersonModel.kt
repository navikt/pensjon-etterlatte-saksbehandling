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
    val adressebeskyttelse: Boolean,
    val adresse: String?,
    val husnummer: String?,
    val husbokstav: String?,
    val postnummer: String?,
    val poststed: String?,
    val statsborgerskap: String?,
    val foedeland: String?,
    val sivilstatus: String?,
    val utland: eyUtland?,
    val rolle: Rolle?


)

data class eyUtland(
    val innflyttingTilNorge: List<eyInnflyttingTilNorge>,
    val utflyttingFraNorge: List<eyUtflyttingFraNorge>
)
data class eyInnflyttingTilNorge(
    val fraflyttingsland: String,
    val dato: String,

)

data class eyUtflyttingFraNorge(
    val tilflyttingsland: String,
    val dato: String,
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
