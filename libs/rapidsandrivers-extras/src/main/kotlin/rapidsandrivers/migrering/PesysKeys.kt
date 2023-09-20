package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import rapidsandrivers.HENDELSE_DATA_KEY

const val FNR_KEY = "fnr"
const val ROLLE_KEY = "rolle"
const val VILKAARSVURDERT_KEY = "vilkaarsvurdert"
const val TRYGDETID_KEY = "trygdetid"
const val MIGRERING_GRUNNLAG_KEY = "migrering_grunnlag"
const val PERSONGALLERI = "persongalleri"
const val PESYS_ID = "pesysId"
const val KILDE = "kilde"

var JsonMessage.hendelseData: MigreringRequest
    get() = objectMapper.treeToValue(this[HENDELSE_DATA_KEY], MigreringRequest::class.java)
    set(name) {
        this[HENDELSE_DATA_KEY] = name
    }

var JsonMessage.persongalleri: Persongalleri
    get() = objectMapper.treeToValue(this[PERSONGALLERI], Persongalleri::class.java)
    set(name) {
        this[PERSONGALLERI] = name
    }

var JsonMessage.pesysId: PesysId
    get() = objectMapper.treeToValue(this[PESYS_ID], PesysId::class.java)
    set(name) {
        this[PESYS_ID] = name
    }

var JsonMessage.kilde: Vedtaksloesning
    get() = objectMapper.treeToValue(this[KILDE], Vedtaksloesning::class.java)
    set(name) {
        this[KILDE] = name
    }
