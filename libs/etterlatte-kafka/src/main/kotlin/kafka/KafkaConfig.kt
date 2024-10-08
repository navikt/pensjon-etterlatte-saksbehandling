package no.nav.etterlatte.kafka

import no.nav.etterlatte.kafka.KafkaKey.KAFKA_BROKERS
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_CREDSTORE_PASSWORD
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_KEYSTORE_PATH
import no.nav.etterlatte.kafka.KafkaKey.KAFKA_TRUSTSTORE_PATH
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.appIsInGCP
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import java.net.InetAddress
import java.util.Properties

interface KafkaConfig {
    fun producerConfig(): Properties
}

fun hentKafkaConfig(env: Miljoevariabler) =
    if (appIsInGCP()) {
        GcpKafkaConfig.fromEnv(env)
    } else {
        LocalKafkaConfig(env[KAFKA_BROKERS]!!)
    }

class GcpKafkaConfig(
    private val bootstrapServers: String,
    private val truststore: String,
    private val truststorePassword: String,
    private val keystoreLocation: String,
    private val keystorePassword: String,
    private val clientId: String,
) : KafkaConfig {
    companion object {
        fun fromEnv(env: Miljoevariabler): KafkaConfig =
            GcpKafkaConfig(
                bootstrapServers = env.requireEnvValue(KAFKA_BROKERS),
                truststore = env.requireEnvValue(KAFKA_TRUSTSTORE_PATH),
                truststorePassword = env.requireEnvValue(KAFKA_CREDSTORE_PASSWORD),
                keystoreLocation = env.requireEnvValue(KAFKA_KEYSTORE_PATH),
                keystorePassword = env.requireEnvValue(KAFKA_CREDSTORE_PASSWORD),
                clientId = InetAddress.getLocalHost().hostName,
            )
    }

    override fun producerConfig() =
        kafkaBaseConfig().apply {
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.CLIENT_ID_CONFIG, "producer-$clientId")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.RETRIES_CONFIG, Int.MAX_VALUE)
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        }

    private fun kafkaBaseConfig() =
        Properties().apply {
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
    private val brokersURL: String,
) : KafkaConfig {
    override fun producerConfig() =
        kafkaBaseConfig().apply {
            put(ProducerConfig.ACKS_CONFIG, "1")
            put(ProducerConfig.CLIENT_ID_CONFIG, "etterlatte-local-kafka")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.RETRIES_CONFIG, Int.MAX_VALUE)
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        }

    private fun kafkaBaseConfig() =
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        }
}

enum class KafkaKey : EnvEnum {
    KAFKA_RAPID_TOPIC,
    KAFKA_BROKERS,
    KAFKA_TRUSTSTORE_PATH,
    KAFKA_CREDSTORE_PASSWORD,
    KAFKA_KEYSTORE_PATH,
    KAFKA_SCHEMA_REGISTRY_USER,
    KAFKA_SCHEMA_REGISTRY_PASSWORD,
    KAFKA_SCHEMA_REGISTRY,
    KAFKA_TARGET_TOPIC,
    ;

    override fun key() = name
}
