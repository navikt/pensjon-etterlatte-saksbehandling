package no.nav.etterlatte.kafka

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SkjermingslesingTest {
    companion object {
        const val PDL_PERSON_TOPIC = "nom.skjermede-personer-status-v1"
        private val kafkaContainer = kafkaContainer(PDL_PERSON_TOPIC)
    }

    @Test
    fun `Les skjermingshendelse og post det til behandlingsapp`() {
        val fnr = "09508229892"
        val producer = spyk(KafkaProducerTestImpl<String>(kafkaContainer, StringSerializer::class.java.canonicalName))
        producer.sendMelding(PDL_PERSON_TOPIC, fnr, "value")

        val behandlingKlient = mockk<BehandlingKlient>()
        every { behandlingKlient.haandterHendelse(any()) } just runs

        val closed = AtomicBoolean(false)

        val kafkaConsumerEgneAnsatte =
            KafkaConsumerEgneAnsatte(
                env =
                    mapOf(
                        "KAFKA_BROKERS" to kafkaContainer.bootstrapServers,
                        "SKJERMING_GROUP_ID" to KafkaContainerHelper.GROUP_ID,
                        "SKJERMING_TOPIC" to PDL_PERSON_TOPIC,
                    ),
                behandlingKlient = behandlingKlient,
                closed = closed,
                kafkaEnvironment = KafkaConsumerEnvironmentTest(),
                pollTimeoutInSeconds = Duration.ofSeconds(4L),
            )
        val thread =
            thread(start = true) {
                while (true) {
                    if (kafkaConsumerEgneAnsatte.getAntallMeldinger() >= 1) {
                        closed.set(true)
                        kafkaConsumerEgneAnsatte.consumer.wakeup()
                        return@thread
                    }
                    Thread.sleep(800L) // Må stå så ikke denne spiser all cpu, tester er egentlig single threaded
                }
            }
        kafkaConsumerEgneAnsatte.stream()
        thread.join()
        verify(exactly = 1) { producer.sendMelding(any(), any(), any()) }
        assertEquals(kafkaConsumerEgneAnsatte.getAntallMeldinger(), 1)
        verify { behandlingKlient.haandterHendelse(any()) }
    }

    class KafkaConsumerEnvironmentTest : KafkaConsumerConfiguration {
        override fun generateKafkaConsumerProperties(env: Map<String, String>): Properties {
            val trettiSekunder = 30000

            val properties =
                Properties().apply {
                    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
                    put(ConsumerConfig.GROUP_ID_CONFIG, env["SKJERMING_GROUP_ID"])
                    put(ConsumerConfig.CLIENT_ID_CONFIG, "etterlatte-egne-ansatte-lytter")
                    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10)
                    put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                    put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, trettiSekunder)
                    put(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, Duration.ofSeconds(40L).toMillis().toInt())
                }
            return properties
        }
    }
}
