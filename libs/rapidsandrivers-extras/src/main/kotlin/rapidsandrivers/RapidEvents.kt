package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.rapidsandrivers.RapidEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.RapidEvents.EKSKLUDERTE_SAKER
import no.nav.etterlatte.rapidsandrivers.RapidEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.RapidEvents.LOEPENDE_FOM
import no.nav.etterlatte.rapidsandrivers.RapidEvents.SPESIFIKKE_SAKER
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.YearMonth

object RapidEvents {
    const val KJOERING = "kjoering"
    const val ANTALL = "antall"
    const val SPESIFIKKE_SAKER = "spesifikke_saker"
    const val EKSKLUDERTE_SAKER = "ekskluderte_saker"
    const val LOEPENDE_FOM = "loepende_fom"
}

var JsonMessage.loependeFom: YearMonth
    get() = YearMonth.parse(this[LOEPENDE_FOM].asText())
    set(value) {
        this[LOEPENDE_FOM] = value.toString()
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

var JsonMessage.saker: List<SakId>
    get() =
        this[SPESIFIKKE_SAKER]
            .asText()
            .tilSeparertListe()
    set(name) {
        this[SPESIFIKKE_SAKER] = name.tilSeparertString()
    }

var JsonMessage.ekskluderteSaker: List<SakId>
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

fun List<SakId>.tilSeparertString() = this.joinToString(";") { it.toString() }
