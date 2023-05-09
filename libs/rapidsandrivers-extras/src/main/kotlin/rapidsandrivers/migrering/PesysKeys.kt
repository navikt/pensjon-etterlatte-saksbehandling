package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage

const val REQUEST = "request"
const val FNR_KEY = "fnr"
const val ROLLE_KEY = "rolle"

var JsonMessage.request: String
    get() = this[REQUEST].toJson()
    set(name) {
        this[REQUEST] = name
    }