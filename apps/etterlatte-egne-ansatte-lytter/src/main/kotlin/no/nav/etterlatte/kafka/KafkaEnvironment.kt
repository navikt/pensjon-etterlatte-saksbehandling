package no.nav.etterlatte.kafka

import org.apache.kafka.common.serialization.StringDeserializer

class KafkaEnvironment :
    Kafkakonfigurasjon<StringDeserializer>(
        groupId = "SKJERMING_GROUP_ID",
        deserializerClass = StringDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
    )
