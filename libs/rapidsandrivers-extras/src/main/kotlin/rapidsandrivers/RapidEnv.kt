package no.nav.etterlatte.rapidsandrivers

import no.nav.helse.rapids_rivers.AivenConfig
import no.nav.helse.rapids_rivers.Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import java.util.Properties

fun getRapidEnv(): Map<String, String> =
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }

fun configFromEnvironment(env: Map<String, String>): Config {
    val gcpConfigAvailable = env.containsKey("KAFKA_BROKERS") && env.containsKey("KAFKA_CREDSTORE_PASSWORD")
    return if (gcpConfigAvailable) {
        AivenConfig.default
    } else {
        LocalKafkaConfig(env)
    }
}

class LocalKafkaConfig(
    private val env: Map<String, String>,
) : Config {
    override fun producerConfig(properties: Properties): Properties =
        properties.apply {
            connectionConfig(this)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.RETRIES_CONFIG, "0")
        }

    override fun consumerConfig(
        groupId: String,
        properties: Properties,
    ): Properties =
        properties.apply {
            connectionConfig(this)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }

    override fun adminConfig(properties: Properties): Properties =
        properties.apply {
            connectionConfig(this)
        }

    private fun connectionConfig(properties: Properties) =
        properties.apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env["KAFKA_BOOTSTRAP_SERVERS"])
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        }
}
