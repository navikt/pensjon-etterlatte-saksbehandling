package no.nav.etterlatte.hendelserpdl

import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.mockk.spyk
import io.mockk.verify
import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.hendelserpdl.common.PersonhendelseKonsument
import no.nav.etterlatte.hendelserpdl.pdl.PdlKlient
import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.lesHendelserFraLeesah
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET
import no.nav.etterlatte.libs.common.toJson
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrationTest {

    private lateinit var pdlKlient: PdlKlient

    @BeforeEach
    fun setup() {
        mockPdlResponse()
    }

    @Test
    fun `skal opprette doedshendelse paa leesah, konsumere den, mappe om og publisere paa rapid`() {
        val env = mapOf(
            "KAFKA_BROKERS" to kafkaEnv.brokersURL,
            "LEESAH_KAFKA_GROUP_ID" to "etterlatte-v1",
            "KAFKA_SCHEMA_REGISTRY" to kafkaEnv.schemaRegistry?.url!!,
            "LEESAH_TOPIC_PERSON" to LEESAH_TOPIC_PERSON
        )

        val leesahKafkaProducer = producerForLeesah()
        val rapidsKafkaProducer = spyk(
            LocalKafkaConfig(kafkaEnv.brokersURL).rapidsAndRiversProducer("etterlatte.dodsmelding")
        )

        val personHendelseFordeler = PersonHendelseFordeler(rapidsKafkaProducer, pdlKlient)
        val personhendelseKonsument = PersonhendelseKonsument(
            LEESAH_TOPIC_PERSON,
            KafkaConsumerEnvironmentTest().generateKafkaConsumerProperties(env),
            personHendelseFordeler
        )

        val personHendelse = Personhendelse().apply {
            hendelseId = "1"
            endringstype = Endringstype.OPPRETTET
            master = ""
            opprettet = Instant.now()
            personidenter = listOf(FNR.value)
            opplysningstype = LeesahOpplysningstype.DOEDSFALL_V1.toString()
            doedsfall = Doedsfall().apply {
                doedsdato = LocalDate.of(2022, 1, 1)
            }
        }

        val forventetMeldingPaaRapid = MeldingSendtPaaRapid(
            eventName = "PDL:PERSONHENDELSE",
            hendelse = LeesahOpplysningstype.DOEDSFALL_V1,
            hendelse_data = Doedshendelse(
                hendelseId = personHendelse.hendelseId,
                endringstype = OPPRETTET,
                fnr = personHendelse.personidenter.first(),
                doedsdato = personHendelse.doedsfall?.doedsdato
            )
        )

        leesahKafkaProducer.send(ProducerRecord(LEESAH_TOPIC_PERSON, 1, "key", personHendelse))

        lesHendelserFraLeesah(personhendelseKonsument)

        verify(exactly = 1, timeout = 5000) {
            rapidsKafkaProducer.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<Doedshendelse> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                }
            )
        }
    }

    class KafkaConsumerEnvironmentTest : KafkaConsumerConfiguration {
        override fun generateKafkaConsumerProperties(env: Map<String, String>): Properties {
            val trettiSekunder = 30000

            val properties = Properties().apply {
                put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaEnv.brokersURL)
                put(ConsumerConfig.GROUP_ID_CONFIG, env["LEESAH_KAFKA_GROUP_ID"])
                put(ConsumerConfig.CLIENT_ID_CONFIG, "etterlatte-pdl-hendelser")
                put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaEnv.schemaRegistry?.url!!)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, trettiSekunder)
                put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
            }
            return properties
        }
    }

    private fun producerForLeesah() = KafkaProducer<String, Personhendelse>(
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaEnv.brokersURL,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaEnv.schemaRegistry?.url,
            ProducerConfig.ACKS_CONFIG to "all"
        )
    )

    private fun mockPdlResponse() {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.startsWith("/")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = mapOf(
                            "folkeregisterident" to FNR.value,
                            "type" to "FOLKEREGISTERIDENT"
                        ).toJson()
                        respond(json, headers = headers)
                    } else {
                        error(request.url.fullPath)
                    }
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        }

        pdlKlient = PdlKlient(httpClient, "http://etterlatte-pdltjenester")
    }

    companion object {
        const val LEESAH_TOPIC_PERSON = "pdl.leesah-v1"

        val kafkaEnv = KafkaEnvironment(
            noOfBrokers = 1,
            topicNames = listOf(LEESAH_TOPIC_PERSON),
            withSecurity = false,
            autoStart = true,
            withSchemaRegistry = true
        )
    }
}