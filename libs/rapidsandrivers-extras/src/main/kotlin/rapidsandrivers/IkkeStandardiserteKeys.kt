package rapidsandrivers

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.toUUID
import java.time.LocalDate
import java.util.*

const val omberegningId = "omberegningId"
const val beregningKey = "beregning"
const val behandlingIdKey = "behandlingId"
const val datoKey = "dato"

var JsonMessage.behandlingId: UUID
    get() = this[behandlingIdKey].asText().toUUID()
    set(name) {
        this[behandlingIdKey] = name
    }

var JsonMessage.dato: LocalDate
    get() = this[datoKey].asLocalDate()
    set(name) {
        this[datoKey] = name
    }