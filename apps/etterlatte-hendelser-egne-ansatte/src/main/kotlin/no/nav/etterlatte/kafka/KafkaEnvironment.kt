package no.nav.etterlatte.kafka

import no.nav.etterlatte.EgneAnsatteKey
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaEnvironment :
    Kafkakonfigurasjon<StringDeserializer>(
        groupId = EgneAnsatteKey.SKJERMING_GROUP_ID,
        deserializerClass = StringDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
    )
