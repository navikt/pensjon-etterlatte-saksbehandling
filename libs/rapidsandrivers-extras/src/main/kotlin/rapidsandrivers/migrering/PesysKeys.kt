package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage

const val REQUEST = "request"

var JsonMessage.request: String
    get() = this[REQUEST].toJson()
    set(name) {
        this[REQUEST] = name
    }