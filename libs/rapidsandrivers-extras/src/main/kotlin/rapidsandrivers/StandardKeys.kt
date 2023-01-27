package no.nav.etterlatte.libs.common.rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.River

const val eventNameKey = "@event_name"
const val behovNameKey = "@behov"
const val correlationIdKey = "@correlation_id"
const val tekniskTidKey = "teknisk_tid"

fun River.eventName(eventName: String) {
    validate { it.demandValue(eventNameKey, eventName) }
}

var JsonMessage.eventName: String
    get() = this[eventNameKey].textValue()
    set(name) {
        this[eventNameKey] = name
    }

fun River.correlationId() {
    validate { it.interestedIn(correlationIdKey) }
}

var JsonMessage.correlationId: String?
    get() = this[correlationIdKey].textValue()
    set(name) {
        name?.also { this[correlationIdKey] = it }
            ?: throw IllegalArgumentException("Kan ikke sette correlationId til null")
    }

fun River.behov(behov: String) {
    validate { it.demandValue(behovNameKey, behov) }
}

var JsonMessage.behov: String
    get() = this[behovNameKey].textValue()
    set(name) {
        this[behovNameKey] = name
    }