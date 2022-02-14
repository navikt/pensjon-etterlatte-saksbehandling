package no.nav.etterlatte.batch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetAddress
import java.time.LocalDateTime

// Stripped down version of JsonMessage from rapids and rivers
open class JsonMessage(
    originalMessage: String,
) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        private const val ReadCountKey = "system_read_count"
        private const val ParticipatingServicesKey = "system_participating_services"

        private val serviceName: String? = System.getenv("NAIS_APP_NAME")
        private val serviceHostname = serviceName?.let { InetAddress.getLocalHost().hostName }

        fun newMessage(map: Map<String, Any> = emptyMap()) =
            objectMapper.writeValueAsString(map).let { JsonMessage(it) }
    }

    private val json: JsonNode
    private val recognizedKeys = mutableMapOf<String, JsonNode>()

    init {
        json =  objectMapper.readTree(originalMessage)
        set(ReadCountKey, json.path(ReadCountKey).asInt(-1) + 1)

        if (serviceName != null && serviceHostname != null) {
            val entry = mapOf(
                "service" to serviceName,
                "instance" to serviceHostname,
                "time" to LocalDateTime.now()
            )
            if (json.path(ParticipatingServicesKey).isMissingOrNull()) set(ParticipatingServicesKey, listOf(entry))
            else (json.path(ParticipatingServicesKey) as ArrayNode).add(objectMapper.valueToTree<JsonNode>(entry))
        }
    }


    operator fun get(key: String): JsonNode =
        requireNotNull(recognizedKeys[key]) { "$key is unknown; keys must be declared as required, forbidden, or interesting" }

    operator fun set(key: String, value: Any) {
        (json as ObjectNode).replace(key, objectMapper.valueToTree<JsonNode>(value).also {
            recognizedKeys[key] = it
        })
    }

    fun toJson(): String = objectMapper.writeValueAsString(json)
}

fun JsonNode.isMissingOrNull() = isMissingNode || isNull
