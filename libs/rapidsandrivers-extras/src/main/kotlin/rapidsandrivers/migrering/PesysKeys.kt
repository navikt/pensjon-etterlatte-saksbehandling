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
const val PERSONGALLERI_KEY = "persongalleri"
const val PESYS_ID_KEY = "pesysId"
const val KILDE_KEY = "kilde"

var JsonMessage.hendelseData: MigreringRequest
    get() = objectMapper.treeToValue(this[HENDELSE_DATA_KEY], MigreringRequest::class.java)
    set(name) {
        this[HENDELSE_DATA_KEY] = name
    }

var JsonMessage.persongalleri: Persongalleri
    get() = objectMapper.treeToValue(this[PERSONGALLERI_KEY], Persongalleri::class.java)
    set(name) {
        this[PERSONGALLERI_KEY] = name
    }

var JsonMessage.pesysId: PesysId
    get() = objectMapper.treeToValue(this[PESYS_ID_KEY], PesysId::class.java)
    set(name) {
        this[PESYS_ID_KEY] = name
    }

var JsonMessage.kilde: Vedtaksloesning
    get() = objectMapper.treeToValue(this[KILDE_KEY], Vedtaksloesning::class.java)
    set(name) {
        this[KILDE_KEY] = name
    }
