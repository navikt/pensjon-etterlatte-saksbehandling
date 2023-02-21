package no.nav.etterlatte.hendelserpdl.leesah

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.util.*

class KafkaConfig(
    private val bootstrapServers: String,
    private val consumerGroupId: String,
    private val clientId: String? = null,
    private val username: String? = null,
    private val password: String? = null,
    private val truststore: String? = null,
    private val truststorePassword: String? = null,
    private val autoOffsetResetConfig: String? = null,
    private val schemaRegistryUrl: String? = null,
    private val autoCommit: Boolean? = false,
    maxIntervalMs: Int? = null,
    maxRecords: Int? = null
) {
    private companion object {
        private const val DefaultMaxRecords = 200
    }

    private val maxPollRecords = maxRecords ?: DefaultMaxRecords
    private val maxPollIntervalMs = maxIntervalMs ?: Duration.ofSeconds(60 + maxPollRecords * 2.toLong()).toMillis()

    private val log = LoggerFactory.getLogger(this::class.java)

    internal fun consumerConfig() = Properties().apply {
        putAll(kafkaBaseConfig())
        put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId)
        clientId?.also { put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-$it") }
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetResetConfig ?: "latest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, if (true == autoCommit) "true" else "false")
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "$maxPollRecords")
        put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "$maxPollIntervalMs")

        schemaRegistryUrl?.apply {
            put("schema.registry.url", this)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
            put("specific.avro.reader", true)
        }
    }

    private fun kafkaBaseConfig() = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        // put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
        if (username != null) {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")

            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule " +
                    "required username=\"$username\" password=\"$password\";"
            )
        }
        if (!truststore.isNullOrBlank()) {
            try {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(truststore).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
                log.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
            } catch (ex: Exception) {
                log.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
            }
        }
    }
}