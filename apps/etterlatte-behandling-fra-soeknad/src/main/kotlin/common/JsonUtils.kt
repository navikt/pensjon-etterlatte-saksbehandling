package no.nav.etterlatte.common

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(JavaTimeModule())

fun Any.toJson(): String = jacksonObjectMapper().writeValueAsString(this)

inline fun <reified T : Any> mapJsonToAny(json: String, failonunknown: Boolean = false): T {
    return objectMapper.let {
        if (failonunknown) it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        else it
    }.readValue(json)
}
