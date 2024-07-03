package no.nav.etterlatte.samordning

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import org.apache.kafka.common.serialization.Deserializer

class KafkaEnvironment :
    Kafkakonfigurasjon<KafkaEnvironmentJsonDeserializer>(
        groupId = "SAMORDNINGVEDTAK_HENDELSE_GROUP_ID",
        deserializerClass = KafkaEnvironmentJsonDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
    )

private val mapper = jacksonObjectMapper()

class KafkaEnvironmentJsonDeserializer : Deserializer<SamordningVedtakHendelse> {
    override fun deserialize(
        topic: String?,
        data: ByteArray,
    ): SamordningVedtakHendelse = mapper.readValue(data)
}
