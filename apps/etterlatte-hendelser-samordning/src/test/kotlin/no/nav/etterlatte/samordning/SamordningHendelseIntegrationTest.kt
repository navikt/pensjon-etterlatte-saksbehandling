package no.nav.etterlatte.samordning

import com.fasterxml.jackson.core.JsonProcessingException
import io.ktor.server.application.Application
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.kafka.KafkaConsumerEnvironmentTest
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import no.nav.etterlatte.kafka.KafkaProducerTestImpl
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.common.objectMapper
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SamordningHendelseIntegrationTest {
    companion object {
        const val SAMORDNINGVEDTAK_HENDELSE_TOPIC = "sam-vedtak-samhandlersvar"
        private val kafkaContainer = kafkaContainer(SAMORDNINGVEDTAK_HENDELSE_TOPIC)
    }

    @Test
    fun `Motta meldinger og sende videre relevante paa river`() {
        val rapidsKafkaProducer =
            spyk(
                LocalKafkaConfig(kafkaContainer.bootstrapServers).rapidsAndRiversProducer("etterlatte.dodsmelding"),
            )

        val konsument =
            SamordningHendelseKonsument(
                topic = SAMORDNINGVEDTAK_HENDELSE_TOPIC,
                kafkaProperties =
                    KafkaConsumerEnvironmentTest().konfigurer(
                        kafkaContainer,
                        KafkaEnvironmentJsonDeserializer::class.java.canonicalName,
                    ),
                handler = SamordningHendelseHandler(rapidsKafkaProducer),
            )

        val produsent = KafkaProducerTestImpl<SamordningVedtakHendelse>(kafkaContainer, SamJsonSerializer::class.java.canonicalName)
        produsent.sendMelding(
            SAMORDNINGVEDTAK_HENDELSE_TOPIC,
            UUID.randomUUID().toString(),
            SamordningVedtakHendelse().apply {
                fagomrade = "PENSJON"
                artTypeKode = "AP2025"
                vedtakId = 100200300L
            },
        )
        produsent.sendMelding(
            SAMORDNINGVEDTAK_HENDELSE_TOPIC,
            UUID.randomUUID().toString(),
            SamordningVedtakHendelse().apply {
                fagomrade = "EYO"
                artTypeKode = "OMS"
                vedtakId = 99900022201L
            },
        )

        startLytting(konsument, LoggerFactory.getLogger(Application::class.java))

        verify(exactly = 1, timeout = 5000) {
            rapidsKafkaProducer.publiser(
                any(),
                match {
                    val hendelse = objectMapper.readTree(it.toJson())
                    hendelse.get("vedtakId").asLong() == 99900022201L
                },
            )
        }
    }
}

class SamJsonSerializer : Serializer<SamordningVedtakHendelse> {
    override fun serialize(
        topic: String,
        data: SamordningVedtakHendelse?,
    ): ByteArray {
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: JsonProcessingException) {
            throw SerializationException("Error serializing JSON message", e)
        }
    }
}
