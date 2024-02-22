package no.nav.etterlatte.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvrokonstanterTest {
    @Test
    fun `vaare hardkodede konstanter i libs matcher med dem i Avro-biblioteket`() {
        assertEquals(
            AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
            Avrokonstanter.BASIC_AUTH_CREDENTIALS_SOURCE,
        )
        assertEquals(
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG,
            Avrokonstanter.SPECIFIC_AVRO_READER_CONFIG,
        )
        assertEquals(
            SchemaRegistryClientConfig.USER_INFO_CONFIG,
            Avrokonstanter.USER_INFO_CONFIG,
        )
        assertEquals(
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
            Avrokonstanter.SCHEMA_REGISTRY_URL_CONFIG,
        )
    }
}
