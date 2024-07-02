package no.nav.etterlatte.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

class KafkaContainerHelper {
    companion object {
        const val CLIENT_ID = "etterlatte-test-v1"
        const val GROUP_ID = "etterlatte-v1"
        const val CONFLUENT_PLATFORM_VERSION = "7.4.3"

        fun kafkaContainer(topic: String) =
            KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:${CONFLUENT_PLATFORM_VERSION}"),
            ).waitingFor(HostPortWaitStrategy())
                .apply {
                    start()
                    val adminClient =
                        AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers))
                    createTopic(adminClient, topic)
                }

        private fun createTopic(
            adminClient: AdminClient,
            vararg topics: String,
        ) {
            val newTopics = topics.map { topic -> NewTopic(topic, 1, 1.toShort()) }
            adminClient.createTopics(newTopics)
        }

        fun KafkaContainer.kafkaKonsument(klientId: String) =
            KafkaConsumer<String, String>(
                mapOf(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID,
                    ConsumerConfig.CLIENT_ID_CONFIG to klientId,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1000",
                    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "PLAINTEXT",
                    SaslConfigs.SASL_MECHANISM to "PLAIN",
                ),
            )
    }
}

class KafkaProducerTestImpl<T>(
    serialiserJson: Boolean,
    kafkaContainer: KafkaContainer,
) {
    private val serializer =
        if (serialiserJson) {
            JsonSerializer<T>()
        } else {
            StringSerializer()
        }
    private val produsent =
        KafkaProducer<String, T>(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to serializer::class.java,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true, // Den sikrer rekkefølge
                ProducerConfig.ACKS_CONFIG to "all", // Den sikrer at data ikke mistes
                ProducerConfig.CLIENT_ID_CONFIG to KafkaContainerHelper.CLIENT_ID,
            ),
        )

    fun sendMelding(
        topic: String,
        nøkkel: String,
        verdi: T,
    ) = runBlocking(context = Dispatchers.IO) {
        produsent.send(ProducerRecord(topic, nøkkel, verdi)).get()
    }
}

class JsonSerializer<T> : Serializer<T> {
    override fun serialize(
        topic: String,
        data: T?,
    ): ByteArray {
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error serializing JSON message", e)
        }
    }
}
