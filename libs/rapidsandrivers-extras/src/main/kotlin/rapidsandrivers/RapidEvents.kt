package rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import rapidsandrivers.RapidEvents.ANTALL
import rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import rapidsandrivers.RapidEvents.KJOERING
import rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER

object RapidEvents {
    const val KJOERING = "kjoering"
    const val ANTALL = "antall"
    const val SPESIFIKKE_SAKER = "spesifikke_saker"
    const val EKSKLUDERTE_SAKER = "ekskluderte_saker"
}

var JsonMessage.antall: Int
    get() = this[ANTALL].asInt()
    set(value) {
        this[ANTALL] = value
    }

var JsonMessage.kjoering: String
    get() = this[KJOERING].asText()
    set(value) {
        this[KJOERING] = value
    }

var JsonMessage.saker: List<Long>
    get() =
        this[SPESIFIKKE_SAKER]
            .asText()
            .tilSeparertListe()
    set(name) {
        this[SPESIFIKKE_SAKER] = name.tilSeparertString()
    }

var JsonMessage.ekskluderteSaker: List<Long>
    get() =
        this[EKSKLUDERTE_SAKER]
            .asText()
            .tilSeparertListe()
    set(name) {
        this[EKSKLUDERTE_SAKER] = name.tilSeparertString()
    }

fun String.tilSeparertListe() =
    trim()
        .split(";")
        .filter { it.isNotEmpty() }
        .map { it.toLong() }

fun List<Long>.tilSeparertString() = this.joinToString(";") { it.toString() }
