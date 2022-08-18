package kafka

import no.nav.etterlatte.kafka.JsonMessage
import org.apache.kafka.common.serialization.Deserializer
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

class JsonMessageDeserializer : Deserializer<JsonMessage> {
    private val stringDeserializer = org.apache.kafka.common.serialization.StringDeserializer()

    override fun configure(configs: Map<String?, *>, isKey: Boolean) {
        stringDeserializer.configure(configs, isKey)
    }

    override fun deserialize(topic: String?, data: ByteArray?): JsonMessage {
        return JsonMessage(stringDeserializer.deserialize(topic, data))
    }
}