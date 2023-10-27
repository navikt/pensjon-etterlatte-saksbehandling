package no.nav.etterlatte.joarkhendelser.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import joarkhendelser.common.JournalfoeringHendelse
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import org.apache.kafka.common.IsolationLevel
import java.util.Locale

class KafkaEnvironment : Kafkakonfigurasjon<KafkaEnvironment.JsonDeserializer>(
    groupId = "JOARK_HENDELSE_GROUP_ID",
    deserializerClass = JsonDeserializer::class.java,
    userInfoConfigKey = SchemaRegistryClientConfig.USER_INFO_CONFIG,
    schemaRegistryUrlConfigKey = AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
    isolationLevelConfig = IsolationLevel.READ_COMMITTED.toString().lowercase(Locale.getDefault()),
) {
    class JsonDeserializer : org.apache.kafka.common.serialization.Deserializer<JournalfoeringHendelse> {
        private val mapper = jacksonObjectMapper()

        override fun deserialize(
            topic: String?,
            data: ByteArray,
        ): JournalfoeringHendelse {
            return mapper.readValue(data)
        }
    }
}
