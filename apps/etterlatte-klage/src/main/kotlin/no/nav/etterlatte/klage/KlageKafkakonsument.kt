package no.nav.etterlatte.klage

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import no.nav.etterlatte.kafka.Kafkakonsument
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration

class KlageKafkakonsument(
    env: Map<String, String>,
    topic: String,
    private val behandlingKlient: BehandlingKlient
) : Kafkakonsument<String>(
    logger = LoggerFactory.getLogger(KlageKafkakonsument::class.java.name),
    consumer = KafkaConsumer<String, String>(KafkaEnvironment().generateKafkaConsumerProperties(env)),
    topic = topic,
    pollTimeoutInSeconds = Duration.ofSeconds(10L)

) {
    override fun stream() {
        stream { meldinger -> meldinger.forEach { behandlingKlient.haandterHendelse(it) } }
    }
}

internal class KafkaEnvironment : Kafkakonfigurasjon<StringDeserializer>(
    groupId = "KLAGE_GROUP_ID",
    deserializerClass = StringDeserializer::class.java,
    userInfoConfigKey = SchemaRegistryClientConfig.USER_INFO_CONFIG,
    schemaRegistryUrlConfigKey = AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
)