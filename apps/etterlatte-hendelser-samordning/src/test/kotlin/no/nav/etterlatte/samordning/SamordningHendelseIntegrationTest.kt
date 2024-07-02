package no.nav.etterlatte.samordning

import io.ktor.server.application.Application
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.kafka.KafkaContainerHelper
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import no.nav.etterlatte.kafka.KafkaProducerTestImpl
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.samordning.KafkaEnvironment.JsonDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SamordningHendelseIntegrationTest {
    private val klientId = "etterlatte-test-v1"

    companion object {
        const val SAMORDNINGVEDTAK_HENDELSE_TOPIC = "sam-vedtak-samhandlersvar"
        private val kafkaContainer = kafkaContainer(SAMORDNINGVEDTAK_HENDELSE_TOPIC)
    }

    @Test
    fun `Motta meldinger og sende videre relevante paa river`() {
        val rapidsKafkaProducer =
            spyk(
                LocalKafkaConfig(kafkaContainer.bootstrapServers).rapidsAndRiversProducer("etterlatte.dodsmelding"),
            )

        val konsument =
            SamordningHendelseKonsument(
                topic = SAMORDNINGVEDTAK_HENDELSE_TOPIC,
                kafkaProperties =
                    KafkaConsumerEnvironmentTest().generateKafkaConsumerProperties(
                        mapOf(
                            "KAFKA_GROUP_ID" to KafkaContainerHelper.GROUP_ID,
                        ),
                    ),
                handler = SamordningHendelseHandler(rapidsKafkaProducer),
            )

        val produsent = KafkaProducerTestImpl<SamordningVedtakHendelse>(true, kafkaContainer)
        produsent.sendMelding(
            SAMORDNINGVEDTAK_HENDELSE_TOPIC,
            1,
            UUID.randomUUID().toString(),
            SamordningVedtakHendelse().apply {
                fagomrade = "PENSJON"
                artTypeKode = "AP2025"
                vedtakId = 100200300L
            },
        )
        produsent.sendMelding(
            SAMORDNINGVEDTAK_HENDELSE_TOPIC,
            1,
            UUID.randomUUID().toString(),
            SamordningVedtakHendelse().apply {
                fagomrade = "EYO"
                artTypeKode = "OMS"
                vedtakId = 99900022201L
            },
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

    class KafkaConsumerEnvironmentTest : KafkaConsumerConfiguration {
        override fun generateKafkaConsumerProperties(env: Map<String, String>): Properties {
            val tiSekunder = 10000

            val properties =
                Properties().apply {
                    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
                    put(ConsumerConfig.GROUP_ID_CONFIG, env["KAFKA_GROUP_ID"])
                    put(ConsumerConfig.CLIENT_ID_CONFIG, "etterlatte-test-v1")
                    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
                    put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, tiSekunder)
                    put(Avrokonstanter.SPECIFIC_AVRO_READER_CONFIG, true)
                }
            return properties
        }
    }
}
