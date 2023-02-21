package rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.toUUID
import java.time.LocalDate
import java.util.*

const val sakIdKey = "sakId"
const val behandlingIdKey = "behandlingId"
const val omberegningIdKey = "omberegningId"
const val beregningKey = "beregning"
const val datoKey = "dato"
const val hendelseDataKey = "hendelse_data"

var JsonMessage.sakId: Long
    get() = this[sakIdKey].asLong()
    set(name) {
        this[sakIdKey] = name
    }

var JsonMessage.behandlingId: UUID
    get() = this[behandlingIdKey].asText().toUUID()
    set(name) {
        this[behandlingIdKey] = name
    }

var JsonMessage.omberegningId: UUID
    get() = this[omberegningIdKey].asText().toUUID()
    set(name) {
        this[omberegningIdKey] = name
    }

var JsonMessage.dato: LocalDate
    get() = this[datoKey].asLocalDate()
    set(name) {
        this[datoKey] = name
    }