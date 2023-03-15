package no.nav.etterlatte.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import java.net.InetAddress
import java.util.*

interface KafkaConfig {
    fun producerConfig(): Properties
}

class GcpKafkaConfig(
    private val bootstrapServers: String,
    private val truststore: String,
    private val truststorePassword: String,
    private val keystoreLocation: String,
    private val keystorePassword: String,
    private val clientId: String
) : KafkaConfig {
    companion object {
        private fun generateInstanceId(env: Map<String, String>): String {
            if (env.containsKey("NAIS_APP_NAME")) return InetAddress.getLocalHost().hostName
            return UUID.randomUUID().toString()
        }
        fun fromEnv(env: Map<String, String>): KafkaConfig = GcpKafkaConfig(
            bootstrapServers = env.getValue("KAFKA_BROKERS"),
            truststore = env.getValue("KAFKA_TRUSTSTORE_PATH"),
            truststorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
            keystoreLocation = env.getValue("KAFKA_KEYSTORE_PATH"),
            keystorePassword = env.getValue("KAFKA_CREDSTORE_PASSWORD"),
            clientId = generateInstanceId(env)
        )
    }
    override fun producerConfig() = kafkaBaseConfig().apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.CLIENT_ID_CONFIG, "producer-$clientId")
        put(ProducerConfig.LINGER_MS_CONFIG, "0")
        put(ProducerConfig.RETRIES_CONFIG, Int.MAX_VALUE)
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private fun kafkaBaseConfig() = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststore)
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation)
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword)
    }
}

class LocalKafkaConfig(
    private val brokersURL: String
) : KafkaConfig {
    override fun producerConfig() = kafkaBaseConfig().apply {
        put(ProducerConfig.ACKS_CONFIG, "1")
        put(ProducerConfig.CLIENT_ID_CONFIG, "etterlatte-local-kafka")
        put(ProducerConfig.LINGER_MS_CONFIG, "0")
        put(ProducerConfig.RETRIES_CONFIG, Int.MAX_VALUE)
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private fun kafkaBaseConfig() = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
    }
}