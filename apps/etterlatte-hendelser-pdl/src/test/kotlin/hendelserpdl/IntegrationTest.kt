package no.nav.etterlatte.hendelserpdl

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.JsonMessage
import no.nav.etterlatte.hendelserpdl.leesah.LivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class IntegrationTest {

    private lateinit var pdlService: PdlService

    companion object {
        val kafkaEnv = KafkaEnvironment(
            noOfBrokers = 1,
            topicNames = listOf("etterlatte.dodsmelding", "aapen-person-pdl-leesah-v1"),
            withSecurity = false,
            // users = listOf(JAASCredential("myP1", "myP1p"),JAASCredential("myC1", "myC1p")),
            autoStart = true,
            withSchemaRegistry = true
        )

        @AfterAll
        @JvmStatic
        fun teardown() {
            kafkaEnv.tearDown()
        }
    }

    @Test
    fun test() {
        val leesahTopic: KafkaProducer<String, Personhendelse> = producerForLeesah()
        val rapid: KafkaConsumer<String, String> = consumerForRapid()
        mockEndpoint("/personidentresponse.json")
        val doedsfall = Doedsfall(LocalDate.of(2022, 1, 1))

        val app = testApp()

        rapid.subscribe(listOf("etterlatte.dodsmelding"))
        app.stream()
        leesahTopic.send(
            ProducerRecord(
                "aapen-person-pdl-leesah-v1",
                Personhendelse(
                    "1",
                    listOf("1234567"),
                    "",
                    Instant.now(),
                    "DOEDSFALL_V1",
                    Endringstype.OPPRETTET,
                    null,
                    null,
                    null,
                    doedsfall,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null

                )
            )
        ).get()

        app.stream()

        rapid.poll(Duration.ofSeconds(4L)).apply {
            assertEquals(1, this.count())
        }.forEach {
            val msg = JsonMessage(it.value(), MessageProblems(it.value()))
            println(it.value())
            msg.interestedIn("hendelse", "hendelse_data", eventNameKey, "system_read_count")
            val doedshendelse = objectMapper.treeToValue<Doedshendelse>(msg["hendelse_data"])
            assertEquals("DOEDSFALL_V1", msg["hendelse"].textValue())
            assertEquals("70078749472", doedshendelse.avdoedFnr)
            assertEquals(doedsfall.doedsdato, doedshendelse.doedsdato)
            assertEquals(no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET, doedshendelse.endringstype)
            assertEquals("PDL:PERSONHENDELSE", msg[eventNameKey].textValue())
            assertEquals(1, msg["system_read_count"].asInt())
        }
    }

    private fun testApp() = FinnDodsmeldinger(
        LivetErEnStroemAvHendelser(
            mapOf(
                "LEESAH_KAFKA_BROKERS" to kafkaEnv.brokersURL,
                "LEESAH_KAFKA_GROUP_ID" to "leesah-consumer",
                "LEESAH_KAFKA_SCHEMA_REGISTRY" to kafkaEnv.schemaRegistry?.url!!
            )
        ),
        Dodsmeldinger(TestConfig(true, mapOf("KAFKA_BROKERS" to kafkaEnv.brokersURL))),
        pdlService
    )

    private fun consumerForRapid() = KafkaConsumer(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaEnv.brokersURL,
            ConsumerConfig.GROUP_ID_CONFIG to "test",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "10",
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "100"
        ),
        StringDeserializer(),
        StringDeserializer()
    )

    private fun producerForLeesah() = KafkaProducer<String, Personhendelse>(
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaEnv.brokersURL,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            "schema.registry.url" to kafkaEnv.schemaRegistry?.url,
            ProducerConfig.ACKS_CONFIG to "all"
        )
    )

    private fun mockEndpoint(jsonUrl: String) {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.startsWith("/")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = javaClass.getResource(jsonUrl)!!.readText()
                        respond(json, headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        }

        pdlService = PdlService(httpClient, "http://etterlatte-pdltjenester")
    }
}