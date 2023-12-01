package no.nav.etterlatte.samordning

import com.fasterxml.jackson.core.JsonProcessingException
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.ktor.server.application.Application
import io.mockk.spyk
import io.mockk.verify
import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.samordning.KafkaEnvironment.JsonDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SamordningHendelseIntegrationTest {
    @Test
    fun `Motta meldinger og sende videre relevante paa river`() {
        val env =
            mapOf(
                "KAFKA_BROKERS" to kafkaEnv.brokersURL,
                "KAFKA_GROUP_ID" to "etterlatte-v1",
                "KAFKA_SCHEMA_REGISTRY" to kafkaEnv.schemaRegistry?.url!!,
                "SAMORDNINGVEDTAK_HENDELSE_TOPIC" to SAMORDNINGVEDTAK_HENDELSE_TOPIC,
            )

        val producerForSamordningVedtakHendelse = producerForSamordningVedtakHendelse()

        val rapidsKafkaProducer =
            spyk(
                LocalKafkaConfig(kafkaEnv.brokersURL).rapidsAndRiversProducer("etterlatte.dodsmelding"),
            )

        val konsument =
            SamordningHendelseKonsument(
                topic = "sam-vedtak-samhandlersvar",
                kafkaProperties = KafkaConsumerEnvironmentTest().generateKafkaConsumerProperties(env),
                handler = SamordningHendelseHandler(rapidsKafkaProducer),
            )

        producerForSamordningVedtakHendelse.send(
            // Denne skal ikke sendes videre på river
            ProducerRecord(
                SAMORDNINGVEDTAK_HENDELSE_TOPIC,
                1,
                UUID.randomUUID().toString(),
                SamordningVedtakHendelse().apply {
                    fagomrade = "PENSJON"
                    artTypeKode = "AP2025"
                    vedtakId = 100200300L
                },
            ),
        )
        producerForSamordningVedtakHendelse.send(
            ProducerRecord(
                SAMORDNINGVEDTAK_HENDELSE_TOPIC,
                1,
                UUID.randomUUID().toString(),
                SamordningVedtakHendelse().apply {
                    fagomrade = "EYO"
                    artTypeKode = "OMS"
                    vedtakId = 99900022201L
                },
            ),
        )

        startLytting(konsument, LoggerFactory.getLogger(Application::class.java))

        verify(exactly = 1, timeout = 5000) {
            rapidsKafkaProducer.publiser(
                any(),
                match {
                    val hendelse = objectMapper.readTree(it.toJson())
                    hendelse.get("vedtakId").asLong() == 99900022201L
                },
            )
        }
    }

    private fun producerForSamordningVedtakHendelse() =
        KafkaProducer<String, SamordningVedtakHendelse>(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaEnv.brokersURL,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to SamJsonSerializer::class.java.canonicalName,
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaEnv.schemaRegistry?.url,
                ProducerConfig.ACKS_CONFIG to "all",
            ),
        )

    class KafkaConsumerEnvironmentTest : KafkaConsumerConfiguration {
        override fun generateKafkaConsumerProperties(env: Map<String, String>): Properties {
            val tiSekunder = 10000

            val properties =
                Properties().apply {
                    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaEnv.brokersURL)
                    put(ConsumerConfig.GROUP_ID_CONFIG, env["KAFKA_GROUP_ID"])
                    put(ConsumerConfig.CLIENT_ID_CONFIG, "etterlatte-test-v1")
                    put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaEnv.schemaRegistry?.url!!)
                    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
                    put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, tiSekunder)
                    put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
                }
            return properties
        }
    }

    companion object {
        const val SAMORDNINGVEDTAK_HENDELSE_TOPIC = "sam-vedtak-samhandlersvar"

        val kafkaEnv =
            KafkaEnvironment(
                noOfBrokers = 1,
                topicNames = listOf(SAMORDNINGVEDTAK_HENDELSE_TOPIC),
                withSecurity = false,
                autoStart = true,
                withSchemaRegistry = true,
            )
    }
}

class SamJsonSerializer : Serializer<SamordningVedtakHendelse> {
    override fun serialize(
        topic: String,
        data: SamordningVedtakHendelse?,
    ): ByteArray {
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: JsonProcessingException) {
            throw SerializationException("Error serializing JSON message", e)
        }
    }
}
