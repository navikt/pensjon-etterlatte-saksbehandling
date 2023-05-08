package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.helse.rapids_rivers.JsonMessage

const val REQUEST = "request"

var JsonMessage.request: String
    get() = this[REQUEST].asText()
    set(name) {
        this[REQUEST] = name
    }