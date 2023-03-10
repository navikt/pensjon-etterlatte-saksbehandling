package no.nav.etterlatte.hendelserpdl.leesah

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.Properties

interface KafkaConsumerConfiguration {
    fun generateKafkaConsumerProperties(env: Map<String, String>): Properties
}

class KafkaEnvironment : KafkaConsumerConfiguration {
    override fun generateKafkaConsumerProperties(env: Map<String, String>): Properties {
        val maxPollInterval = Duration.ofMinutes(8L).toMillis().toInt()
        val properties = Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env["KAFKA_BROKERS"])
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, env["KAFKA_TRUSTSTORE_PATH"])
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env["KAFKA_CREDSTORE_PASSWORD"])
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, env["KAFKA_KEYSTORE_PATH"])
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, env["KAFKA_CREDSTORE_PASSWORD"])

            put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, env["KAFKA_CREDSTORE_PASSWORD"])
            // Nais doc: Password needed to use the keystore and truststore

            put(ConsumerConfig.GROUP_ID_CONFIG, env["LEESAH_KAFKA_GROUP_ID"])
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
            put(ConsumerConfig.CLIENT_ID_CONFIG, env["NAIS_APP_NAME"])
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollInterval)
            put(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, Duration.ofSeconds(20L).toMillis().toInt())

            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)

            put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
            put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")

            put(
                KafkaAvroDeserializerConfig.USER_INFO_CONFIG,
                "${env["KAFKA_SCHEMA_REGISTRY_USER"]}:${env["KAFKA_SCHEMA_REGISTRY_PASSWORD"]}"
            )
            put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, env["KAFKA_SCHEMA_REGISTRY"])
            put(
                "schema.registry.basic.auth.user.info",
                "${env["KAFKA_SCHEMA_REGISTRY_USER"]}:${env["KAFKA_SCHEMA_REGISTRY_PASSWORD"]}"
            )
        }
        return properties
    }
}