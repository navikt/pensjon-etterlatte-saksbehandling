package no.nav.etterlatte.hendelserufoere.config

import no.nav.etterlatte.hendelserufoere.config.UfoereKey.UFOERE_KAFKA_GROUP_ID
import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaEnvironment :
    Kafkakonfigurasjon<StringDeserializer>(
        groupId = UFOERE_KAFKA_GROUP_ID,
        deserializerClass = StringDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
    )
