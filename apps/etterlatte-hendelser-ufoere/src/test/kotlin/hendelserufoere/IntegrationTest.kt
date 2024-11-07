package no.nav.etterlatte.hendelserufoere

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.mockk.spyk
import no.nav.etterlatte.hendelserufoere.common.UfoerehendelseKonsument
import no.nav.etterlatte.kafka.KafkaConsumerEnvironmentTest
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import no.nav.etterlatte.kafka.LocalKafkaConfig
import no.nav.etterlatte.kafka.rapidsAndRiversProducer
import no.nav.etterlatte.lesHendelserFraUfoere
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrationTest {
    @Test
    fun `skal opprette ufoerehendelse paa ufoere, konsumere den, håndtere den og sende videre`() {
        val rapidsKafkaProducer = spyk(LocalKafkaConfig(kafkaContainer.bootstrapServers).rapidsAndRiversProducer("etterlatte.dodsmelding"))

        val ufoerehendelseKonsument =
            UfoerehendelseKonsument(
                UFOERE_TOPIC,
                KafkaConsumerEnvironmentTest().konfigurer(
                    kafkaContainer,
                    KafkaAvroDeserializer::class.java.canonicalName,
                ),
                UfoereHendelseFordeler(rapidsKafkaProducer),
            )

        lesHendelserFraUfoere(ufoerehendelseKonsument)
    }

    companion object {
        const val UFOERE_TOPIC = "ufoere-topic"
        private val kafkaContainer = kafkaContainer(UFOERE_TOPIC)
    }
}
