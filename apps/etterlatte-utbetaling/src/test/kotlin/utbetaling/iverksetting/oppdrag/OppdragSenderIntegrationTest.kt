package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.utbetaling.DummyJmsConnectionFactory
import no.nav.etterlatte.utbetaling.TestContainers.ibmMQContainer
import no.nav.etterlatte.utbetaling.config.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.utbetaling.oppdrag
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppdragSenderIntegrationTest {

    private val jmsConnectionFactory: EtterlatteJmsConnectionFactory = DummyJmsConnectionFactory()
    private lateinit var oppdragSender: OppdragSender

    @BeforeAll
    fun beforeAll() {
        oppdragSender = OppdragSender(
            jmsConnectionFactory = jmsConnectionFactory,
            queue = "DEV.QUEUE.1",
            replyQueue = "DEV.QUEUE.1"
        )
    }

    @Test
    fun `skal legge oppdrag på køen`() {
        val oppdrag = oppdrag(utbetaling(vedtakId = 1))

        val oppdragXml = oppdragSender.sendOppdrag(oppdrag)

        assertNotNull(oppdragXml)
    }

    @AfterAll
    fun afterAll() {
        jmsConnectionFactory.stop()
        ibmMQContainer.stop()
    }
}