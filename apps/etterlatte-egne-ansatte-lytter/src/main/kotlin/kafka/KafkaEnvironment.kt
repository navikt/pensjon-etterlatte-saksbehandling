package no.nav.etterlatte.kafka

import org.apache.kafka.common.serialization.StringDeserializer

class KafkaEnvironment : Kafkakonfigurasjon(
    groupId = "SKJERMING_GROUP_ID",
    deserializerClass = StringDeserializer::class
)