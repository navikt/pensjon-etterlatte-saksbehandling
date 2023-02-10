package no.nav.etterlatte.hendelserpdl

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer

class AppConfig {
    private val JAVA_KEYSTORE = "jks"
    private val PKCS12 = "PKCS12"
    val env = System.getenv().toMutableMap().apply { put("DELAYED_START", "true") }

    private fun envOrThrow(envVar: String) =
        env[envVar] ?: throw IllegalStateException("$envVar er påkrevd miljøvariabel")

    fun producerConfig() = mutableMapOf<String, Any>(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to envOrThrow("KAFKA_BROKERS"),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
        ProducerConfig.ACKS_CONFIG to "all",

        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", // Disable server host name verification
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to JAVA_KEYSTORE,
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to PKCS12,
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_TRUSTSTORE_PATH"),
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_KEYSTORE_PATH"),
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD")
    )
}