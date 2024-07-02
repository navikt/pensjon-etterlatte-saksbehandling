package no.nav.etterlatte.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.SCHEMA_REGISTRY_URL
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Properties

class KafkaContainerHelper {
    companion object {
        const val CLIENT_ID = "etterlatte-test-v1"
        const val GROUP_ID = "etterlatte-v1"
        const val SCHEMA_REGISTRY_URL = "mock://mock-registry"
        private const val CONFLUENT_PLATFORM_VERSION = "7.4.3"

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
    }
}

class KafkaProducerTestImpl<T>(
    kafkaContainer: KafkaContainer,
    serialiseringsklasse: String,
    schemaRegistryUrl: String? = null,
) {
    private val produsent =
        KafkaProducer<String, T>(
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to serialiseringsklasse,
                Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true, // Den sikrer rekkefølge
                ProducerConfig.ACKS_CONFIG to "all", // Den sikrer at data ikke mistes
                ProducerConfig.CLIENT_ID_CONFIG to KafkaContainerHelper.CLIENT_ID,
                Avrokonstanter.SPECIFIC_AVRO_READER_CONFIG to true,
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

class KafkaConsumerEnvironmentTest {
    fun konfigurer(
        kafkaContainer: KafkaContainer,
        deserializerClass: String,
    ): Properties {
        val trettiSekunder = Duration.ofSeconds(30).toMillis().toInt()

        val properties =
            Properties().apply {
                put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, KafkaContainerHelper.GROUP_ID)
                put(ConsumerConfig.CLIENT_ID_CONFIG, KafkaContainerHelper.CLIENT_ID)
                put(Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializerClass)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, trettiSekunder)
                put(Avrokonstanter.SPECIFIC_AVRO_READER_CONFIG, true)
            }
        return properties
    }
}
