package no.nav.etterlatte.hendelserpdl

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
import no.nav.etterlatte.hendelserpdl.leesah.KafkaConsumerConfiguration
import no.nav.etterlatte.hendelserpdl.leesah.KafkaConsumerHendelserPdl
import no.nav.etterlatte.hendelserpdl.leesah.LivsHendelserTilRapid
import no.nav.etterlatte.hendelserpdl.leesah.PersonHendelseFordeler
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrationTest {

    private lateinit var pdlService: PdlService

    companion object {
        val pdlPersonTopic = "pdl.leesah-v1"
        val kafkaEnv = KafkaEnvironment(
            noOfBrokers = 1,
            topicNames = listOf(pdlPersonTopic),
            withSecurity = false,
            autoStart = true,
            withSchemaRegistry = true
        )
    }

    @Test
    fun testDoedshendelse() {
        val leesahKafkaProducer: KafkaProducer<String, Personhendelse> = producerForLeesah()
        mockEndpoint()
        val rapidsKafkaProducer = spyk(
            LocalKafkaConfig(kafkaEnv.brokersURL).rapidsAndRiversProducer("etterlatte.dodsmelding")
        )
        val livsHendelserTilRapid = spyk(LivsHendelserTilRapid(rapidsKafkaProducer))
        val closed = AtomicBoolean()
        closed.set(false)
        val kafkaConsumerWrapper = KafkaConsumerHendelserPdl(
            PersonHendelseFordeler(livsHendelserTilRapid, pdlService),
            mapOf(
                "KAFKA_BROKERS" to kafkaEnv.brokersURL,
                "LEESAH_KAFKA_GROUP_ID" to "etterlatte-v1",
                "KAFKA_SCHEMA_REGISTRY" to kafkaEnv.schemaRegistry?.url!!,
                "LEESAH_TOPIC_PERSON" to pdlPersonTopic
            ),
            closed,
            KafkaConsumerEnvironmentTest()
        )

        val doedsfall = Doedsfall(LocalDate.of(2022, 1, 1))
        leesahKafkaProducer.send(
            ProducerRecord(
                pdlPersonTopic,
                1,
                "x",
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
        )

        val utflyttingFraNorge = UtflyttingFraNorge(
            "Sverige",
            null,
            LocalDate.of(2022, 8, 8)
        )
        leesahKafkaProducer.send(
            ProducerRecord(
                pdlPersonTopic,
                1,
                "x",
                Personhendelse(
                    "1",
                    listOf("1234567"),
                    "",
                    Instant.now(),
                    "UTFLYTTING_FRA_NORGE",
                    Endringstype.OPPRETTET,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    utflyttingFraNorge,
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
        )

        val forelderBarnRelasjon = ForelderBarnRelasjon(
            "12345678911",
            "MOR",
            "BARN",
            null
        )
        leesahKafkaProducer.send(
            ProducerRecord(
                pdlPersonTopic,
                Personhendelse(
                    "1",
                    listOf("1234567"),
                    "",
                    Instant.now(),
                    "FORELDERBARNRELASJON_V1",
                    Endringstype.OPPRETTET,
                    null,
                    null,
                    null,
                    null,
                    null,
                    forelderBarnRelasjon,
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
        )
        val PDLfnr = "70078749472"

        val thread = thread(start = true) {
            while (true) {
                if (kafkaConsumerWrapper.getAntallMeldinger() >= 3) {
                    closed.set(true)
                    kafkaConsumerWrapper.getConsumer().wakeup()
                    return@thread
                }
                Thread.sleep(800L) // Må stå så ikke denne spiser all cpu, tester er egentlig single threaded
            }
        }
        kafkaConsumerWrapper.stream()
        thread.join()

        verify(exactly = 3) { rapidsKafkaProducer.publiser(any(), any()) }
        verify {
            livsHendelserTilRapid.personErDod(
                PDLfnr,
                doedsfall.doedsdato.toString(),
                no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET
            )
        }

        verify {
            livsHendelserTilRapid.personUtflyttingFraNorge(
                PDLfnr,
                utflyttingFraNorge.tilflyttingsland,
                utflyttingFraNorge.tilflyttingsstedIUtlandet,
                utflyttingFraNorge.utflyttingsdato.toString(),
                no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET
            )
        }

        verify {
            livsHendelserTilRapid.forelderBarnRelasjon(
                PDLfnr,
                forelderBarnRelasjon.relatertPersonsIdent,
                forelderBarnRelasjon.relatertPersonsRolle,
                forelderBarnRelasjon.minRolleForPerson,
                forelderBarnRelasjon.relatertPersonUtenFolkeregisteridentifikator?.toString(),
                no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET
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

    private fun mockEndpoint() {
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.fullPath.startsWith("/")) {
                        val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        val json = "{\n" +
                            "  \"folkeregisterident\": \"70078749472\"\n" +
                            "}"
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