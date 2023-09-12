
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.common.KafkaEnvironment
import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.kafka.KafkaConsumerConfiguration
import no.nav.etterlatte.kafka.KafkaConsumerEgneAnsatte
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class SkjermingslesingTest {

    companion object {
        val pdlPersonTopic = "nom.skjermede-personer-status-v1"
        val kafkaEnv = KafkaEnvironment(
            noOfBrokers = 1,
            topicNames = listOf(pdlPersonTopic),
            withSecurity = false,
            autoStart = true,
            withSchemaRegistry = true
        )
    }

    @Test
    fun `Les skjermingshendelse og post det til behandlingsapp`() {
        val skjermingsProducer: KafkaProducer<String, String> = spyk(generateSkjermingsProducer())
        val fnr = "70078749472"
        skjermingsProducer.send(ProducerRecord(pdlPersonTopic, fnr, "value"))
        val behandlingKlient = mockk<BehandlingKlient>()
        every { behandlingKlient.haandterHendelse(any()) } just runs

        val closed = AtomicBoolean(false)

        val kafkaConsumerEgneAnsatte = KafkaConsumerEgneAnsatte(
            env = mapOf(
                "KAFKA_BROKERS" to kafkaEnv.brokersURL,
                "SKJERMING_GROUP_ID" to "etterlatte-v1",
                "KAFKA_SCHEMA_REGISTRY" to kafkaEnv.schemaRegistry?.url!!,
                "SKJERMING_TOPIC" to pdlPersonTopic
            ),
            behandlingKlient = behandlingKlient,
            closed = closed,
            kafkaEnvironment = KafkaConsumerEnvironmentTest(),
            pollTimeoutInSeconds = Duration.ofSeconds(4L)
        )
        val thread = thread(start = true) {
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
        verify(exactly = 1) { skjermingsProducer.send(any(), any()) }
        assertEquals(kafkaConsumerEgneAnsatte.getAntallMeldinger(), 1)
        verify { behandlingKlient.haandterHendelse(any()) }
    }

    private fun generateSkjermingsProducer() = KafkaProducer<String, String>(
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaEnv.brokersURL,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaEnv.schemaRegistry?.url,
            ProducerConfig.ACKS_CONFIG to "all"
        )
    )

    class KafkaConsumerEnvironmentTest : KafkaConsumerConfiguration {
        override fun generateKafkaConsumerProperties(env: Map<String, String>): Properties {
            val trettiSekunder = 30000

            val properties = Properties().apply {
                put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaEnv.brokersURL)
                put(ConsumerConfig.GROUP_ID_CONFIG, env["SKJERMING_GROUP_ID"])
                put(ConsumerConfig.CLIENT_ID_CONFIG, "etterlatte-egne-ansatte-lytter")
                put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaEnv.schemaRegistry?.url!!)
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