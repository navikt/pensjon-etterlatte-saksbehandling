package no.nav.etterlatte.samordning

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import no.nav.etterlatte.libs.common.logging.sikkerlogger

class KafkaEnvironment : Kafkakonfigurasjon<KafkaEnvironment.JsonDeserializer>(
    groupId = "SAMORDNINGVEDTAK_HENDELSE_GROUP_ID",
    deserializerClass = JsonDeserializer::class.java,
    userInfoConfigKey = SchemaRegistryClientConfig.USER_INFO_CONFIG,
    schemaRegistryUrlConfigKey = AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
) {
    class JsonDeserializer : org.apache.kafka.common.serialization.Deserializer<SamordningVedtakHendelse> {
        override fun deserialize(
            topic: String?,
            data: ByteArray,
        ): SamordningVedtakHendelse {
            sikkerLogg.info("Pre-deserialize: {}", String(data))
            return mapper.readValue(data)
        }
    }

    companion object {
        private val mapper = jacksonObjectMapper()
        private val sikkerLogg = sikkerlogger()
    }
}
