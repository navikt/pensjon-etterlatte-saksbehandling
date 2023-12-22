import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import no.nav.etterlatte.kafka.Avrokonstanter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AvrokonstanterTest {
    @Test
    fun `vaare hardkodede konstanter i libs matcher med dem i Avro-biblioteket`() {
        Assertions.assertEquals(
            AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE,
            Avrokonstanter.BASIC_AUTH_CREDENTIALS_SOURCE,
        )
        Assertions.assertEquals(
            io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG,
            Avrokonstanter.SPECIFIC_AVRO_READER_CONFIG,
        )
    }
}
