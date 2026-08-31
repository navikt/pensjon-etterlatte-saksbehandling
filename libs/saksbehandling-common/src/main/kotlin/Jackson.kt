package no.nav.etterlatte.libs.common

import tools.jackson.core.StreamWriteFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

val objectMapper: ObjectMapper =
    jacksonMapperBuilder()
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
        .build()

fun serialize(value: Any): String = objectMapper.writeValueAsString(value)

inline fun <reified T> deserialize(value: String): T = objectMapper.readValue(value)

fun Any.toJson(): String = objectMapper.writeValueAsString(this)

fun Any.toJsonNode(): JsonNode = objectMapper.valueToTree(this)

fun Any.toObjectNode(): ObjectNode = objectMapper.valueToTree(this)
