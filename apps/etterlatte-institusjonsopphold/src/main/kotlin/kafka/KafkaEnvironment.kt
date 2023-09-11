package no.nav.etterlatte.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.IsolationLevel
import java.util.*

class KafkaEnvironment : Kafkakonfigurasjon(
    groupId = "INSTITUSJONSOPPHOLD_GROUP_ID",
    deserializerClass = JsonDeserializer::class,
    extra = {
        it.put(
            ConsumerConfig.ISOLATION_LEVEL_CONFIG,
            IsolationLevel.READ_COMMITTED.toString().lowercase(Locale.getDefault())
        )
    },
    SchemaRegistryClientConfig.USER_INFO_CONFIG,
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
) {

    class JsonDeserializer : org.apache.kafka.common.serialization.Deserializer<KafkaOppholdHendelse> {
        private val mapper = jacksonObjectMapper()

        override fun deserialize(topic: String?, data: ByteArray): KafkaOppholdHendelse {
            return mapper.readValue(data)
        }
    }
}