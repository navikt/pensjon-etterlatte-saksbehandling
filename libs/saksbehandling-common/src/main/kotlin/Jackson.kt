package no.nav.etterlatte.libs.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

val objectMapper: ObjectMapper =
    JsonMapper
        .builder()
        .addModule(JavaTimeModule())
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        .build()

fun serialize(value: Any): String = objectMapper.writeValueAsString(value)

inline fun <reified T> deserialize(value: String): T = objectMapper.readValue(value)

fun Any.toJson(): String = objectMapper.writeValueAsString(this)

fun Any.toJsonNode(): JsonNode = objectMapper.valueToTree(this)

fun Any.toObjectNode(): ObjectNode = objectMapper.valueToTree(this)
