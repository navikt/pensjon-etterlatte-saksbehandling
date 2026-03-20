package no.nav.etterlatte.libs.common

import tools.jackson.core.StreamWriteFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue

val objectMapper: ObjectMapper =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        // Preserve Jackson 2.x behavior for enums and property ordering
        .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
        .disable(EnumFeature.READ_ENUMS_USING_TO_STRING)
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
        .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
        .disable(DateTimeFeature.ONE_BASED_MONTHS)
        .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
        .build()

fun serialize(value: Any): String = objectMapper.writeValueAsString(value)

inline fun <reified T> deserialize(value: String): T = objectMapper.readValue(value)

fun Any.toJson(): String =
    if (this is com.fasterxml.jackson.databind.JsonNode) {
        // Jackson 2 JsonNode (from rapids-and-rivers) - use its own toString() to get proper JSON
        this.toString()
    } else {
        objectMapper.writeValueAsString(this)
    }

fun Any.toJsonNode(): JsonNode =
    if (this is com.fasterxml.jackson.databind.JsonNode) {
        objectMapper.readTree(this.toString())
    } else {
        objectMapper.valueToTree(this)
    }

fun Any.toObjectNode(): ObjectNode =
    if (this is com.fasterxml.jackson.databind.JsonNode) {
        objectMapper.readTree(this.toString()) as ObjectNode
    } else {
        objectMapper.valueToTree(this)
    }
