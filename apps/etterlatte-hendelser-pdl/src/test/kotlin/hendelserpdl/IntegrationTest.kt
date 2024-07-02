package no.nav.etterlatte.hendelserpdl

import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.serializers.KafkaAvroDeserializer
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
import no.nav.etterlatte.hendelserpdl.common.PersonhendelseKonsument
import no.nav.etterlatte.hendelserpdl.pdl.PdlTjenesterKlient
import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.kafka.KafkaContainerHelper
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.SCHEMA_REGISTRY_URL
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import no.nav.etterlatte.kafka.KafkaProducerTestImpl
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.lesHendelserFraLeesah
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype.OPPRETTET
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelserKeys
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.LocalDate
import java.util.Properties

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrationTest {
    private val pdlTjenesterKlient: PdlTjenesterKlient = mockPdlResponse()

    @Test
    fun `skal opprette doedshendelse paa leesah, konsumere den, mappe om og publisere paa rapid`() {
        val rapidsKafkaProducer = spyk(LocalKafkaConfig(kafkaContainer.bootstrapServers).rapidsAndRiversProducer("etterlatte.dodsmelding"))

        val personhendelseKonsument =
            PersonhendelseKonsument(
                LEESAH_TOPIC_PERSON,
                KafkaConsumerEnvironmentTest().generateKafkaConsumerProperties(
                    mapOf(
                        "LEESAH_KAFKA_GROUP_ID" to KafkaContainerHelper.GROUP_ID,
                    ),
                ),
                PersonHendelseFordeler(rapidsKafkaProducer, pdlTjenesterKlient),
            )

        val personHendelse =
            Personhendelse().apply {
                hendelseId = "1"
                endringstype = Endringstype.OPPRETTET
                master = ""
                opprettet = Instant.now()
                personidenter = listOf(AVDOED_FOEDSELSNUMMER.value)
                opplysningstype = LeesahOpplysningstype.DOEDSFALL_V1.toString()
                doedsfall =
                    Doedsfall().apply {
                        doedsdato = LocalDate.of(2022, 1, 1)
                    }
            }

        val forventetMeldingPaaRapid =
            MeldingSendtPaaRapid(
                eventName = PdlHendelserKeys.PERSONHENDELSE.lagEventnameForType(),
                hendelse = LeesahOpplysningstype.DOEDSFALL_V1,
                hendelse_data =
                    DoedshendelsePdl(
                        hendelseId = personHendelse.hendelseId,
                        endringstype = OPPRETTET,
                        fnr = personHendelse.personidenter.first(),
                        doedsdato = personHendelse.doedsfall?.doedsdato,
                    ),
            )

        val producerForLeesah =
            KafkaProducerTestImpl<Personhendelse>(
                kafkaContainer,
                serialiseringsklasse = KafkaAvroSerializer::class.java.canonicalName,
                schemaRegistryUrl = SCHEMA_REGISTRY_URL,
            )
        producerForLeesah.sendMelding(LEESAH_TOPIC_PERSON, "key", personHendelse)

        lesHendelserFraLeesah(personhendelseKonsument)

        verify(exactly = 1, timeout = 5000) {
            rapidsKafkaProducer.publiser(
                any(),
                match {
                    val hendelse: MeldingSendtPaaRapid<DoedshendelsePdl> = objectMapper.readValue(it.toJson())
                    hendelse == forventetMeldingPaaRapid
                },
            )
        }
    }

    class KafkaConsumerEnvironmentTest : KafkaConsumerConfiguration {
        override fun generateKafkaConsumerProperties(env: Map<String, String>): Properties {
            val trettiSekunder = 30000

            val properties =
                Properties().apply {
                    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
                    put(ConsumerConfig.GROUP_ID_CONFIG, env["LEESAH_KAFKA_GROUP_ID"])
                    put(ConsumerConfig.CLIENT_ID_CONFIG, KafkaContainerHelper.CLIENT_ID)
                    put(Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL)
                    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
                    put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                    put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, trettiSekunder)
                    put(Avrokonstanter.SPECIFIC_AVRO_READER_CONFIG, true)
                }
            return properties
        }
    }

    private fun mockPdlResponse(): PdlTjenesterKlient {
        val httpClient =
            HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        if (request.url.fullPath.startsWith("/")) {
                            val headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            val json =
                                mapOf(
                                    "folkeregisterident" to AVDOED_FOEDSELSNUMMER.value,
                                    "type" to "FOLKEREGISTERIDENT",
                                ).toJson()
                            respond(json, headers = headers)
                        } else {
                            error(request.url.fullPath)
                        }
                    }
                }
                install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            }

        return PdlTjenesterKlient(httpClient, "http://etterlatte-pdltjenester")
    }

    companion object {
        const val LEESAH_TOPIC_PERSON = "pdl.leesah-v1"
        private val kafkaContainer = kafkaContainer(LEESAH_TOPIC_PERSON)
    }
}
