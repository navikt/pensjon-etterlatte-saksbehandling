package no.nav.etterlatte.libs.common.rapidsandrivers

import no.nav.etterlatte.libs.common.event.EventnameHendelseType
import no.nav.etterlatte.libs.common.toJson
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
const val SKAL_SENDE_BREV = "skal_sende_brev"
const val REVURDERING_AARSAK = "revurdering_aarsak"
const val FEILMELDING_KEY = "feilmelding"

fun River.eventName(eventName: String) {
    validate { it.demandValue(EVENT_NAME_KEY, eventName) }
}

fun JsonMessage.setEventNameForHendelseType(eventnameHendelseType: EventnameHendelseType) {
    this[EVENT_NAME_KEY] = eventnameHendelseType.lagEventnameForType()
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

var JsonMessage.feilmelding: String
    get() = this[FEILMELDING_KEY].toJson()
    set(name) {
        this[FEILMELDING_KEY] = name
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
