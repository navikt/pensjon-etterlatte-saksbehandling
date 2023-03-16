package rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.toUUID
import java.time.LocalDate
import java.util.*

const val SAK_ID_KEY = "sakId"
const val BEHANDLING_ID_KEY = "behandlingId"
const val BEREGNING_KEY = "beregning"
const val DATO_KEY = "dato"
const val HENDELSE_DATA_KEY = "hendelse_data"
const val BEHANDLING_VI_OMREGNER_FRA_KEY = "behandling_vi_omregner_fra"
const val TILBAKESTILT_KEY = "false"

var JsonMessage.sakId: Long
    get() = this[SAK_ID_KEY].asLong()
    set(name) {
        this[SAK_ID_KEY] = name
    }

var JsonMessage.behandlingId: UUID
    get() = this[BEHANDLING_ID_KEY].asText().toUUID()
    set(name) {
        this[BEHANDLING_ID_KEY] = name
    }

var JsonMessage.dato: LocalDate
    get() = this[DATO_KEY].asLocalDate()
    set(name) {
        this[DATO_KEY] = name
    }

var JsonMessage.tilbakestilt: Boolean
    get() = this[TILBAKESTILT_KEY].asBoolean()
    set(name) {
        this[TILBAKESTILT_KEY] = name
    }