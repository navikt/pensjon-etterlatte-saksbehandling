package no.nav.etterlatte.kafka

import no.nav.common.KafkaEnvironment
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.Properties

class ApplicationTest {
    @Test
    fun verdikjedetest() {
        val topicname = "test_topic"
        val embeddedKafkaEnvironment =
            KafkaEnvironment(
                autoStart = false,
                noOfBrokers = 1,
                topicInfos = listOf(KafkaEnvironment.TopicInfo(name = topicname, partitions = 1)),
                withSchemaRegistry = false,
                withSecurity = false,
                brokerConfigOverrides =
                    Properties().apply {
                        this["auto.leader.rebalance.enable"] = "false"
                        this["group.initial.rebalance.delay.ms"] =
                            "1" // Avoid waiting for new consumers to join group before first rebalancing (default 3000ms)
                    },
            )

        embeddedKafkaEnvironment.start()

        val kafkaConfig = EmbeddedKafkaConfig(embeddedKafkaEnvironment.brokersURL.substringAfterLast("/"))
        val consumer = KafkaConsumer(kafkaConfig.consumer(), StringDeserializer(), StringDeserializer())
        consumer.subscribe(mutableListOf(topicname))

        val kafkaProducer =
            KafkaProdusentImpl<String, String>(
                KafkaProducer(kafkaConfig.producerConfig(), StringSerializer(), StringSerializer()),
                topicname,
            )

        val offset = kafkaProducer.publiser("nøkkel", "verdi")

        consumer.poll(2000).also { assertFalse(it.isEmpty) }.forEach {
            assertEquals("nøkkel", it.key())
            assertEquals("verdi", it.value())
            assertEquals(offset.second, it.offset())
            assertEquals(offset.first, it.partition())
        }
    }
}

class EmbeddedKafkaConfig(
    private val bootstrapServers: String,
) : KafkaConfig {
    override fun producerConfig() =
        kafkaBaseConfig().apply {
            put(ProducerConfig.ACKS_CONFIG, "1")
            put(ProducerConfig.CLIENT_ID_CONFIG, "etterlatte-post-til-kafka")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.RETRIES_CONFIG, Int.MAX_VALUE)
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        }

    private fun kafkaBaseConfig() =
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        }

    fun consumer() =
        kafkaBaseConfig().apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, "etterlatte-post-til-kafka")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        }
}
