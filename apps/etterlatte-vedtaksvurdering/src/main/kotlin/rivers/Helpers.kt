package no.nav.etterlatte.rivers

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.*

internal fun JsonNode.asUUID() = UUID.fromString(asText())
fun JsonMessage.keep(vararg keys: String) = keys.map { it to get(it) }
