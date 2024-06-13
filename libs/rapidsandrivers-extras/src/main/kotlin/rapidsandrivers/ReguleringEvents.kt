package no.nav.etterlatte.rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage

object ReguleringEvents {
    const val DATO = "dato"
    const val KJOERING = "kjoering"
    const val ANTALL = "antall"
    const val SPESIFIKKE_SAKER = "spesifikke_saker"
}

var JsonMessage.saker: List<Long>
    get() =
        this[ReguleringEvents.SPESIFIKKE_SAKER]
            .asText()
            .trim()
            .split(";")
            .filter { it.isNotEmpty() }
            .map { it.toLong() }
    set(name) {
        this[ReguleringEvents.SPESIFIKKE_SAKER] = name.joinToString(";") { it.toString() }
    }
