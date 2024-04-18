package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage

const val FNR_KEY = "fnr"
const val VILKAARSVURDERT_KEY = "vilkaarsvurdert"
const val PESYS_ID_KEY = "pesysId"
const val KILDE_KEY = "kilde"
const val FIKS_BREV_MIGRERING = "fiksBrevMigrering"

var JsonMessage.pesysId: PesysId
    get() = objectMapper.treeToValue(this[PESYS_ID_KEY], PesysId::class.java)
    set(name) {
        this[PESYS_ID_KEY] = name
    }
