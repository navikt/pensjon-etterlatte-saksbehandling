package no.nav.etterlatte.joarkhendelser.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import org.apache.kafka.common.IsolationLevel
import java.util.Locale

class KafkaEnvironment :
    Kafkakonfigurasjon<KafkaAvroDeserializer>(
        groupId = "JOARK_HENDELSE_GROUP_ID",
        deserializerClass = KafkaAvroDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
        isolationLevelConfig = IsolationLevel.READ_COMMITTED.toString().lowercase(Locale.getDefault()),
        specificAvroReaderConfig = true,
    )
