package no.nav.etterlatte.libs.common.rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.River

const val EVENT_NAME_KEY = "@event_name"
const val BEHOV_NAME_KEY = "@behov"
const val CORRELATION_ID_KEY = "@correlation_id"
const val TEKNISK_TID_KEY = "teknisk_tid"

fun River.eventName(eventName: String) {
    validate { it.demandValue(EVENT_NAME_KEY, eventName) }
}

var JsonMessage.eventName: String
    get() = this[EVENT_NAME_KEY].textValue()
    set(name) {
        this[EVENT_NAME_KEY] = name
    }

fun River.correlationId() {
    validate { it.interestedIn(CORRELATION_ID_KEY) }
}

var JsonMessage.correlationId: String?
    get() = this[CORRELATION_ID_KEY].textValue()
    set(name) {
        name?.also { this[CORRELATION_ID_KEY] = it }
            ?: throw IllegalArgumentException("Kan ikke sette correlationId til null")
    }