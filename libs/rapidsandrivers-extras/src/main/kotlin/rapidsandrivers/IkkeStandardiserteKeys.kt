package rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.toUUID
import java.time.LocalDate
import java.util.*

const val SAK_ID_KEY = "sakId"
const val BEHANDLING_ID_KEY = "behandlingId"
const val OMBEREGNING_ID_KEY = "omberegningId"
const val BEREGNING_KEY = "beregning"
const val DATO_KEY = "dato"
const val HENDELSE_DATA_KEY = "hendelse_data"

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

var JsonMessage.omberegningId: UUID
    get() = this[OMBEREGNING_ID_KEY].asText().toUUID()
    set(name) {
        this[OMBEREGNING_ID_KEY] = name
    }

var JsonMessage.dato: LocalDate
    get() = this[DATO_KEY].asLocalDate()
    set(name) {
        this[DATO_KEY] = name
    }