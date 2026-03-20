package no.nav.etterlatte.samordning

import no.nav.etterlatte.kafka.Avrokonstanter
import no.nav.etterlatte.kafka.Kafkakonfigurasjon
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.samordning.SamordningKey.SAMORDNINGVEDTAK_HENDELSE_GROUP_ID
import org.apache.kafka.common.serialization.Deserializer
import tools.jackson.module.kotlin.readValue

class KafkaEnvironment :
    Kafkakonfigurasjon<KafkaEnvironmentJsonDeserializer>(
        groupId = SAMORDNINGVEDTAK_HENDELSE_GROUP_ID,
        deserializerClass = KafkaEnvironmentJsonDeserializer::class.java,
        userInfoConfigKey = Avrokonstanter.USER_INFO_CONFIG,
        schemaRegistryUrlConfigKey = Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
    )

private val mapper = objectMapper

class KafkaEnvironmentJsonDeserializer : Deserializer<SamordningVedtakHendelse> {
    override fun deserialize(
        topic: String?,
        data: ByteArray,
    ): SamordningVedtakHendelse = mapper.readValue(data)
}
