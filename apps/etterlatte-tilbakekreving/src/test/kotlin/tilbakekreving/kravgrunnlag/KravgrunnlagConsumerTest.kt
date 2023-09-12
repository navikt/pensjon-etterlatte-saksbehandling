package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.testsupport.TestContainers
import no.nav.etterlatte.testsupport.readFile
import no.nav.etterlatte.tilbakekreving.config.JmsConnectionFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KravgrunnlagConsumerTest {

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var connectionFactory: JmsConnectionFactory
    private lateinit var kravgrunnlagConsumer: KravgrunnlagConsumer
    private lateinit var kravgrunnlagService: KravgrunnlagService

    @BeforeAll
    fun beforeAll() {
        ibmMQContainer.start()

        connectionFactory = JmsConnectionFactory(
            hostname = ibmMQContainer.host,
            port = ibmMQContainer.firstMappedPort,
            queueManager = "QM1",
            channel = "DEV.ADMIN.SVRCONN",
            username = "admin",
            password = "passw0rd"
        )

        kravgrunnlagService = mockk(relaxed = true)
        kravgrunnlagConsumer = KravgrunnlagConsumer(connectionFactory, QUEUE, kravgrunnlagService).also { it.start() }
    }

    @Test
    fun `skal motta kravgrunnlag og opprette tilbakekreving`() {
        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(timeout = TIMEOUT, exactly = 1) {
            kravgrunnlagService.opprettTilbakekreving(match { it.kravgrunnlagId == BigInteger.valueOf(302004) })
        }
    }

    @Test
    fun `skal motta kravgrunnlag paa nytt dersom noe feiler`() {
        every { kravgrunnlagService.opprettTilbakekreving(any()) } throws Exception("Noe feilet") andThen Unit
        simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten()

        verify(timeout = TIMEOUT, exactly = 1) {
            kravgrunnlagService.opprettTilbakekreving(match { it.kravgrunnlagId == BigInteger.valueOf(302004) })
        }
    }

    @AfterAll
    fun afterAll() {
        connectionFactory.stop()
        ibmMQContainer.stop()
    }

    private fun simulerKravgrunnlagsmeldingFraTilbakekrevingskomponenten() {
        connectionFactory.connection().createSession().use { session ->
            val producer = session.createProducer(session.createQueue(QUEUE))
            val kravgrunnlag = readFile("/kravgrunnlag.xml")
            val message = session.createTextMessage(kravgrunnlag)
            producer.send(message)
        }
    }

    companion object {
        const val QUEUE = "DEV.QUEUE.2"
        const val TIMEOUT: Long = 5000
    }
}