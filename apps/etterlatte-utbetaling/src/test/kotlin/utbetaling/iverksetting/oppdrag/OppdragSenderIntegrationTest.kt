package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import no.nav.etterlatte.libs.testdata.ChipsetCheck
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.oppdrag
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.DisabledIf
import org.testcontainers.junit.jupiter.Container

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIf(value = ChipsetCheck.erM1EllerM2)
internal class OppdragSenderIntegrationTest {

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var jmsConnectionFactory: EtterlatteJmsConnectionFactory
    private lateinit var oppdragSender: OppdragSender

    @BeforeAll
    fun beforeAll() {
        ibmMQContainer.start()

        jmsConnectionFactory = JmsConnectionFactory(
            hostname = ibmMQContainer.host,
            port = ibmMQContainer.firstMappedPort,
            queueManager = "QM1",
            channel = "DEV.ADMIN.SVRCONN",
            username = "admin",
            password = "passw0rd"
        )

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