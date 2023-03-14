package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.batch.payload
import no.nav.etterlatte.kafka.GcpKafkaConfig
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.kafka.standardProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*
import kotlin.system.exitProcess

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

val aremark_person = "12101376212"
val logger: Logger = LoggerFactory.getLogger("BEY001")

fun main() {
    logger.info("Batch startet")
    val env = System.getenv()

    val topic = env.getValue("KAFKA_TARGET_TOPIC")
    logger.info("Konfig lest, oppretter kafka-produsent")

    val producer = GcpKafkaConfig.fromEnv(env).standardProducer(topic)

    sendMelding(
        payload(aremark_person),
        producer
    )
    logger.info("Batch avslutter")
    exitProcess(0)
}

internal fun sendMelding(
    melding: String,
    producer: KafkaProdusent<String, String>
) {
    val startMillis = System.currentTimeMillis()
    logger.info("Publiserer melding")
    producer.publiser("0", createRecord(melding))
    producer.close()

    logger.info("melding publisert på ${(System.currentTimeMillis() - startMillis) / 1000}s")
}

private fun createRecord(input: String) = JsonMessage.newMessage(
    mapOf(
        "@event_name" to "soeknad_innsendt",
        "@skjema_info" to objectMapper.readValue<ObjectNode>(input),
        "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
        "@template" to "soeknad",
        "@fnr_soeker" to aremark_person,
        "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L).toString()
    )
).toJson()