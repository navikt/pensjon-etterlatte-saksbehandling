package no.nav.etterlatte.libs.common

import java.time.Clock

class FeilAntallTiderException(
    message: String,
) : Exception(message)

class UgyldigTidException(
    message: String,
) : Exception(message)

data class OpeningHours(
    val start: Int,
    val slutt: Int,
) {
    companion object {
        fun of(openingHours: String): OpeningHours {
            val startOgSlutt = openingHours.split("-").toList()
            if (startOgSlutt.size != 2) {
                throw FeilAntallTiderException("Fikk ${startOgSlutt.size} tider, skulle hatt 2")
            } else {
                val start = startOgSlutt[0].toInt()
                val slutt = startOgSlutt[1].toInt()
                validateTime(start, slutt)
                return OpeningHours(start, slutt)
            }
        }

        fun validateTime(
            timeStart: Int,
            timeStop: Int,
        ) {
            if (timeStart > 23 || timeStop > 23 || (timeStop == timeStart)) {
                throw UgyldigTidException("Ugydlig tid oppgitt fra start $timeStart slutt $timeStop")
            }
            if (timeStop < timeStart) {
                throw UgyldigTidException("Ugydlig tid oppgitt. Start må være før slutt, fra start $timeStart slutt $timeStop")
            }
        }
    }

    fun isOpen(klokke: Clock): Boolean {
        val time = klokke.instant().atZone(klokke.zone).hour
        return time in start until slutt
    }
}
