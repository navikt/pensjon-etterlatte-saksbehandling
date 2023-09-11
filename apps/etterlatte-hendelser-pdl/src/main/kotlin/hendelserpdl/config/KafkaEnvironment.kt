package no.nav.etterlatte.hendelserpdl.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.IsolationLevel
import java.util.*

class KafkaEnvironment : Kafkakonfigurasjon<KafkaAvroDeserializer>(
    groupId = "LEESAH_KAFKA_GROUP_ID",
    deserializerClass = KafkaAvroDeserializer::class.java,
    extra = {
        it.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
        it.put(
            ConsumerConfig.ISOLATION_LEVEL_CONFIG,
            IsolationLevel.READ_COMMITTED.toString().lowercase(Locale.getDefault())
        )
    },
    userInfoConfigKey = KafkaAvroDeserializerConfig.USER_INFO_CONFIG,
    schemaRegistryUrlConfigKey = KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG
)