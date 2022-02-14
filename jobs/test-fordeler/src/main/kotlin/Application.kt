package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.batch.JsonMessage
import no.nav.etterlatte.batch.KafkaConfig
import no.nav.etterlatte.batch.payload
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*
import kotlin.system.exitProcess

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

//Dette er fnr for "Aremark Testperson" som er en syntetisk bruker som finnes i produksjon
val aremark_person = "10108000398"
val logger:Logger = LoggerFactory.getLogger("BEY001")

fun main() {
    logger.info("Batch startet")
    val env = System.getenv()

    val config = KafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BROKERS"),
        truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
        truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
        keystoreLocation = env.getValue("KAFKA_KEYSTORE_PATH"),
        keystorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD")
    )
    val topic = env.getValue("KAFKA_TARGET_TOPIC")
    logger.info("Konfig lest, oppretter kafka-produsent")

    val producer = KafkaProducer(config.producerConfig(), StringSerializer(), StringSerializer())

    sendMelding(
        payload(aremark_person),
        producer,
        topic
    )
    logger.info("Batch avslutter")
    exitProcess(0)
}

internal fun sendMelding(
    melding: String,
    producer: Producer<String, String>,
    topic: String
) {
    val startMillis = System.currentTimeMillis()
    logger.info("Publiserer melding")

    producer.send(createRecord(melding, topic)).get()


    producer.flush()
    producer.close()

    logger.info("melding publisert på ${(System.currentTimeMillis() - startMillis) / 1000}s")
}

private fun createRecord(input: String, topic: String): ProducerRecord<String, String> {
    val message = JsonMessage.newMessage(mapOf(
        "@event_name" to "soeknad_innsendt",
        "@skjema_info" to objectMapper.readValue<ObjectNode>(input),
        "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
        "@template" to "soeknad",
        "@fnr_soeker" to aremark_person,
        "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L).toString()
    ))
    return ProducerRecord(topic, "0", message.toJson())
}
