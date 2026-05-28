package no.nav.etterlatte.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.kafka.KafkaContainerHelper.Companion.kafkaContainer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SkjermingslesingTest {
    companion object {
        const val PDL_PERSON_TOPIC = "nom.skjermede-personer-status-v1"
        private val kafkaContainer = kafkaContainer(PDL_PERSON_TOPIC)
    }

    @Test
    fun `Les skjermingshendelse og post det til behandlingsapp`() {
        val fnr = "09508229892"
        val producer = spyk(KafkaProducerTestImpl<String>(kafkaContainer, StringSerializer::class.java.canonicalName))
        producer.sendMelding(PDL_PERSON_TOPIC, fnr, "value")

        val latch = CountDownLatch(1)
        val behandlingKlient = mockk<BehandlingKlient>()
        every { behandlingKlient.haandterHendelse(any()) } answers { latch.countDown() }

        val closed = AtomicBoolean(false)

        val kafkaConsumerEgneAnsatte =
            KafkaConsumerEgneAnsatte(
                topic = PDL_PERSON_TOPIC,
                behandlingKlient = behandlingKlient,
                closed = closed,
                kafkaProperties = KafkaConsumerEnvironmentTest().konfigurer(kafkaContainer, StringDeserializer::class.java.canonicalName),
                pollTimeoutInSeconds = Duration.ofSeconds(4L),
            )
        val thread =
            thread(start = true) {
                latch.await(10, TimeUnit.SECONDS)
                closed.set(true)
                kafkaConsumerEgneAnsatte.consumer.wakeup()
            }
        kafkaConsumerEgneAnsatte.stream()
        thread.join()
        verify(exactly = 1) { producer.sendMelding(any(), any(), any()) }
        verify(exactly = 1) { behandlingKlient.haandterHendelse(any()) }
    }
}
