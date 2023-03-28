package no.nav.etterlatte.kafka

import org.apache.kafka.common.serialization.Serializer

class JsonMessageSerializer : Serializer<JsonMessage> {
    private val stringSerializer = org.apache.kafka.common.serialization.StringSerializer()

    override fun configure(configs: Map<String?, *>, isKey: Boolean) {
        stringSerializer.configure(configs, isKey)
    }

    override fun serialize(topic: String, data: JsonMessage): ByteArray {
        return stringSerializer.serialize(topic, data.toJson())
    }
}