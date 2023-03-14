package no.nav.etterlatte.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.net.InetAddress
import java.time.LocalDateTime
import java.util.*

fun interface RandomIdGenerator {
    companion object {
        internal val Default = RandomIdGenerator { UUID.randomUUID().toString() }
    }
    fun generateId(): String
}

// Stripped down version of JsonMessage from rapids and rivers
open class JsonMessage(
    originalMessage: String,
    randomIdGenerator: RandomIdGenerator? = null
) {
    private val idGenerator = randomIdGenerator ?: RandomIdGenerator.Default
    val id: String

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        private const val nestedKeySeparator = '.'
        private const val IdKey = "@id"
        private const val OpprettetKey = "@opprettet"
        private const val EventNameKey = "@event_name"
        private const val NeedKey = "@behov"
        private const val ReadCountKey = "system_read_count"
        private const val ParticipatingServicesKey = "system_participating_services"

        private val serviceName: String? = System.getenv("NAIS_APP_NAME")
        private val serviceImage: String? = System.getenv("NAIS_APP_IMAGE")
        private val serviceHostname = serviceName?.let { InetAddress.getLocalHost().hostName }

        fun newMessage(map: Map<String, Any> = emptyMap(), randomIdGenerator: RandomIdGenerator? = null) =
            objectMapper.writeValueAsString(map).let { JsonMessage(it, randomIdGenerator) }
        fun newMessage(
            eventName: String,
            map: Map<String, Any> = emptyMap(),
            randomIdGenerator: RandomIdGenerator? = null
        ) = newMessage(
            mapOf(EventNameKey to eventName) + map,
            randomIdGenerator
        )
        fun newNeed(
            behov: Collection<String>,
            map: Map<String, Any> = emptyMap(),
            randomIdGenerator: RandomIdGenerator? = null
        ) = newMessage(
            "behov",
            mapOf(
                "@behovId" to UUID.randomUUID(),
                NeedKey to behov
            ) + map,
            randomIdGenerator
        )

        private fun initializeOrSetParticipatingServices(node: JsonNode, id: String, opprettet: LocalDateTime) {
            val entry = mutableMapOf(
                "id" to id,
                "time" to "$opprettet"
            ).apply {
                compute("service") { _, _ -> serviceName }
                compute("instance") { _, _ -> serviceHostname }
                compute("image") { _, _ -> serviceImage }
            }
            if (node.path(ParticipatingServicesKey).isMissingOrNull()) {
                (node as ObjectNode).putArray(
                    ParticipatingServicesKey
                ).add(objectMapper.valueToTree<ObjectNode>(entry))
            } else {
                (node.path(ParticipatingServicesKey) as ArrayNode).add(objectMapper.valueToTree<JsonNode>(entry))
            }
        }
    }

    private val json: JsonNode
    private val recognizedKeys = mutableMapOf<String, JsonNode>()

    init {
        json = objectMapper.readTree(originalMessage)

        id = json.path("@id").takeUnless { it.isMissingOrNull() }?.asText() ?: idGenerator.generateId().also {
            set("@id", it)
        }
        val opprettet = Tidspunkt.now().toLocalDatetimeUTC()
        if (!json.hasNonNull("@opprettet")) set(OpprettetKey, opprettet)
        set(ReadCountKey, json.path(ReadCountKey).asInt(-1) + 1)
        initializeOrSetParticipatingServices(json, id, opprettet)
    }

    private fun node(path: String): JsonNode {
        if (!path.contains(nestedKeySeparator)) return json.path(path)
        return path.split(nestedKeySeparator).fold(json) { result, key ->
            result.path(key)
        }
    }

    operator fun get(key: String): JsonNode =
        requireNotNull(recognizedKeys[key]) {
            "$key is unknown; keys must be declared as required, forbidden, or interesting"
        }

    operator fun set(key: String, value: Any) {
        (json as ObjectNode).replace(
            key,
            objectMapper.valueToTree<JsonNode>(value).also {
                recognizedKeys[key] = it
            }
        )
    }

    fun toJson(): String = objectMapper.writeValueAsString(json)
}

fun JsonNode.isMissingOrNull() = isMissingNode || isNull