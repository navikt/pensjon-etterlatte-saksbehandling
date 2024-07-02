package no.nav.etterlatte.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

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
