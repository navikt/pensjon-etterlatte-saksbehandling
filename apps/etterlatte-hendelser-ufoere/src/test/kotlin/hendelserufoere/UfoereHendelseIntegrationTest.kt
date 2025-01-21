package no.nav.etterlatte.hendelserufoere

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.hendelserufoere.common.UfoerehendelseKonsument
import no.nav.etterlatte.kafka.KafkaConsumerEnvironmentTest
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.SCHEMA_REGISTRY_URL
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import no.nav.etterlatte.kafka.KafkaProducerTestImpl
import no.nav.etterlatte.lesHendelserFraUfoere
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class UfoereHendelseIntegrationTest {
    private val behandlingKlient = mockk<BehandlingKlient>()

    @Test
    fun `skal opprette ufoerehendelse paa topicen, konsumere den, og sende videre til behandling`() {
        coEvery { behandlingKlient.postTilBehandling(any()) } just runs

        val ufoerehendelseKonsument =
            UfoerehendelseKonsument(
                UFOERE_TOPIC,
                KafkaConsumerEnvironmentTest().konfigurer(
                    kafkaContainer,
                    StringDeserializer::class.java.canonicalName,
                ),
                UfoereHendelseFordeler(behandlingKlient),
            )

        val ufoerehendelseProdusent =
            KafkaProducerTestImpl<String>(
                kafkaContainer,
                serialiseringsklasse = StringSerializer::class.java.canonicalName,
                schemaRegistryUrl = SCHEMA_REGISTRY_URL,
            )

        val hendelse =
            UfoereHendelse(
                personIdent = SOEKER_FOEDSELSNUMMER.value,
                fodselsdato = LocalDate.parse("2000-01-01"),
                virkningsdato = LocalDate.parse("2020-01-01"),
                vedtaksType = VedtaksType.INNV,
            )

        ufoerehendelseProdusent.sendMelding(UFOERE_TOPIC, "test-key", hendelse.toJson())

        lesHendelserFraUfoere(ufoerehendelseKonsument)

        coVerify(exactly = 1, timeout = 5000) {
            behandlingKlient.postTilBehandling(hendelse)
        }

        confirmVerified(behandlingKlient)
    }

    private companion object {
        private const val UFOERE_TOPIC = "ufoere-topic"
        private val kafkaContainer = kafkaContainer(UFOERE_TOPIC)
    }
}
