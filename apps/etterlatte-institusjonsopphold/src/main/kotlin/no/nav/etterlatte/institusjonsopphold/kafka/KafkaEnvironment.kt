package no.nav.etterlatte.institusjonsopphold.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.institusjonsopphold.kafka.InstitusjonsoppholdKey.INSTITUSJONSOPPHOLD_GROUP_ID
import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import org.apache.kafka.common.IsolationLevel
import org.apache.kafka.common.serialization.Deserializer
import java.util.Locale

class KafkaEnvironment :
    Kafkakonfigurasjon<KafkaEnvironment.JsonDeserializer>(
        groupId = INSTITUSJONSOPPHOLD_GROUP_ID,
        deserializerClass = JsonDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
        isolationLevelConfig = IsolationLevel.READ_COMMITTED.toString().lowercase(Locale.getDefault()),
    ) {
    class JsonDeserializer : Deserializer<KafkaOppholdHendelse> {
        private val mapper = jacksonObjectMapper()

        override fun deserialize(
            topic: String?,
            data: ByteArray,
        ): KafkaOppholdHendelse = mapper.readValue(data)
    }
}
