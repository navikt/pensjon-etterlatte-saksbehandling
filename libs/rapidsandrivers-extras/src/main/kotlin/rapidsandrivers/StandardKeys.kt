package no.nav.etterlatte.libs.common.rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.River

const val EVENT_NAME_KEY = "@event_name"
const val BEHOV_NAME_KEY = "@behov"
const val CORRELATION_ID_KEY = "@correlation_id"
const val TEKNISK_TID_KEY = "teknisk_tid"
const val FEILENDE_STEG = "feilende_steg"
const val SOEKNAD_ID_KEY = "soeknad_id"
const val SAK_TYPE_KEY = "sak_type"
const val FEILENDE_KRITERIER_KEY = "feilende_kriterier"
const val GYLDIG_FOR_BEHANDLING_KEY = "gyldig_for_behandling"

fun River.eventName(eventName: String) {
    validate { it.demandValue(EVENT_NAME_KEY, eventName) }
}

var JsonMessage.eventName: String
    get() = this[EVENT_NAME_KEY].textValue()
    set(name) {
        this[EVENT_NAME_KEY] = name
    }

var JsonMessage.feilendeSteg: String
    get() = this[FEILENDE_STEG].textValue()
    set(name) {
        this[FEILENDE_STEG] = name
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