package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import rapidsandrivers.HENDELSE_DATA_KEY

const val REQUEST = "request"
const val FNR_KEY = "fnr"
const val ROLLE_KEY = "rolle"
const val VILKAARSVURDERT_KEY = "vilkaarsvurdert"
const val TRYGDETID_KEY = "trygdetid"
const val MIGRERING_GRUNNLAG_KEY = "migrering_grunnlag"
const val SAKTYPE_KEY = "sakType"
const val OPPLYSNING_KEY = "opplysning"
const val BEHOV_KEY = "@behov"

var JsonMessage.request: String
    get() = this[REQUEST].toJson()
    set(name) {
        this[REQUEST] = name
    }

var JsonMessage.hendelseData: MigreringRequest
    get() = objectMapper.treeToValue(this[HENDELSE_DATA_KEY], MigreringRequest::class.java)
    set(name) {
        this[HENDELSE_DATA_KEY] = name
    }