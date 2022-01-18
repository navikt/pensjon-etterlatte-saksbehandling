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
    val utland: Utland?,
    val rolle: Rolle?


)

data class Utland(
    val innflyttingTilNorge: List<InnflyttingTilNorge>,
    val utflyttingFraNorge: List<UtflyttingFraNorge>
)
data class InnflyttingTilNorge(
    val fraflyttingsland: String,
    val dato: String,

)

data class UtflyttingFraNorge(
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
