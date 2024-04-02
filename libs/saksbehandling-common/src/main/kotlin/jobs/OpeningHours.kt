package no.nav.etterlatte.libs.common

import java.time.Clock

data class OpeningHours(val start: Int, val slutt: Int) {
    companion object {
        fun of(openingHours: String): OpeningHours {
            openingHours.split("-").toList()
                .also { require(it.size == 2) }
                .let { return OpeningHours(it[0].toInt(), it[1].toInt()) }
        }
    }

    fun isOpen(klokke: Clock): Boolean {
        val time = klokke.instant().atZone(klokke.zone).hour
        return time in start until slutt
    }
}
