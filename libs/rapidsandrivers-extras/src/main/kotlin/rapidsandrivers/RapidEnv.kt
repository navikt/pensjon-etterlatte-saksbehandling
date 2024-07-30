package no.nav.etterlatte.rapidsandrivers

import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.NaisKey.NAIS_APP_NAME
import no.nav.etterlatte.rapidsandrivers.RapidKey.KAFKA_BOOTSTRAP_SERVERS
import no.nav.etterlatte.rapidsandrivers.RapidKey.KAFKA_BROKERS
import no.nav.etterlatte.rapidsandrivers.RapidKey.KAFKA_CREDSTORE_PASSWORD
import no.nav.helse.rapids_rivers.AivenConfig
import no.nav.helse.rapids_rivers.Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import java.util.Properties

fun getRapidEnv(): Miljoevariabler =
    Miljoevariabler
        .systemEnv()
        .append(RapidKey.KAFKA_CONSUMER_GROUP_ID) { it[NAIS_APP_NAME]!!.replace("-", "") }

fun configFromEnvironment(env: Miljoevariabler): Config {
    val gcpConfigAvailable = env.containsKey(KAFKA_BROKERS) && env.containsKey(KAFKA_CREDSTORE_PASSWORD)
    return if (gcpConfigAvailable) {
        AivenConfig.default
    } else {
        LocalKafkaConfig(env)
    }
}

class LocalKafkaConfig(
    private val env: Miljoevariabler,
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
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env[KAFKA_BOOTSTRAP_SERVERS])
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        }
}

enum class RapidKey : EnvEnum {
    KAFKA_CONSUMER_GROUP_ID,
    KAFKA_BROKERS,
    KAFKA_CREDSTORE_PASSWORD,
    KAFKA_BOOTSTRAP_SERVERS,
    ;

    override fun key() = name
}
