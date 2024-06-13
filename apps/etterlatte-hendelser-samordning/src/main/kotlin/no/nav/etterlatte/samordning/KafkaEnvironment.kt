package no.nav.etterlatte.samordning

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.Kafkakonfigurasjon

class KafkaEnvironment :
    Kafkakonfigurasjon<KafkaEnvironment.JsonDeserializer>(
        groupId = "SAMORDNINGVEDTAK_HENDELSE_GROUP_ID",
        deserializerClass = JsonDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
    ) {
    class JsonDeserializer : org.apache.kafka.common.serialization.Deserializer<SamordningVedtakHendelse> {
        override fun deserialize(
            topic: String?,
            data: ByteArray,
        ): SamordningVedtakHendelse = mapper.readValue(data)
    }

    companion object {
        private val mapper = jacksonObjectMapper()
    }
}
