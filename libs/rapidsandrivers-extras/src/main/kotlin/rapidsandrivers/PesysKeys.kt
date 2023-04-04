package rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage

const val PESYS_SAK_ID_KEY = "pesysSakId"

var JsonMessage.pesysSakId: String
    get() = this[PESYS_SAK_ID_KEY].asText()
    set(name) {
        this[PESYS_SAK_ID_KEY] = name
    }