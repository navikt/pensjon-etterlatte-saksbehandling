package no.nav.etterlatte.hendelserufoere

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.mockk.mockk
import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.hendelserufoere.common.UfoerehendelseKonsument
import no.nav.etterlatte.kafka.KafkaConsumerEnvironmentTest
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import no.nav.etterlatte.lesHendelserFraUfoere
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrationTest {
    private val behandlingKlient = mockk<BehandlingKlient>()

    @Test
    fun `skal opprette ufoerehendelse paa ufoere, konsumere den, håndtere den og sende videre`() {
        val ufoerehendelseKonsument =
            UfoerehendelseKonsument(
                UFOERE_TOPIC,
                KafkaConsumerEnvironmentTest().konfigurer(
                    kafkaContainer,
                    KafkaAvroDeserializer::class.java.canonicalName,
                ),
                UfoereHendelseFordeler(behandlingKlient),
            )

        lesHendelserFraUfoere(ufoerehendelseKonsument)
    }

    companion object {
        const val UFOERE_TOPIC = "ufoere-topic"
        private val kafkaContainer = kafkaContainer(UFOERE_TOPIC)
    }
}
