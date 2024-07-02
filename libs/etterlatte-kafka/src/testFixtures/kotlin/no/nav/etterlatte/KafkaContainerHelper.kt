package no.nav.etterlatte.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
        const val GROUP_ID = "etterlatte-v1"

        fun kafkaContainer(topic: String) =
            KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:7.4.3"),
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

        fun <T> KafkaContainer.kafkaProducer(
            klientId: String,
            serialiserer: Serializer<T>,
        ) = object : KafkaProdusent<T> {
            private val produsent =
                KafkaProducer<String, T>(
                    mapOf(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to this@kafkaProducer.bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to serialiserer::class.java,
                        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true, // Den sikrer rekkefølge
                        ProducerConfig.ACKS_CONFIG to "all", // Den sikrer at data ikke mistes
                        ProducerConfig.CLIENT_ID_CONFIG to klientId,
                    ),
                )

            override fun sendMelding(
                topic: String,
                partition: Int,
                nøkkel: String,
                verdi: T,
            ) {
                runBlocking(context = Dispatchers.IO) {
                    produsent.send(ProducerRecord(topic, nøkkel, verdi)).get()
                }
            }

            override fun sendMelding(
                topic: String,
                nøkkel: String,
                verdi: T,
            ) = sendMelding(topic, 1, nøkkel, verdi)
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

interface KafkaProdusent<T> {
    fun sendMelding(
        topic: String,
        nøkkel: String,
        verdi: T,
    )

    fun sendMelding(
        topic: String,
        partition: Int,
        nøkkel: String,
        verdi: T,
    )
}
