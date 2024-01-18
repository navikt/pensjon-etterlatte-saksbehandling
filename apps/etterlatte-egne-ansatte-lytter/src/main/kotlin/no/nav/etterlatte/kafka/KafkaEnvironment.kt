package no.nav.etterlatte.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaEnvironment : Kafkakonfigurasjon<StringDeserializer>(
    groupId = "SKJERMING_GROUP_ID",
    deserializerClass = StringDeserializer::class.java,
    userInfoConfigKey = SchemaRegistryClientConfig.USER_INFO_CONFIG,
    schemaRegistryUrlConfigKey = AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
)
