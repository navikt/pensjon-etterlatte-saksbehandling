package no.nav.etterlatte.avstemming

import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.utbetalingsoppdrag
import no.nav.etterlatte.util.TestContainers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvstemmingSenderIntegrationTest {

    @Container private val ibmMQContainer = TestContainers.ibmMQContainer
    private lateinit var jmsConnectionFactory: JmsConnectionFactory
    private lateinit var avstemmingSender: AvstemmingSender

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

        avstemmingSender = AvstemmingSender(
            jmsConnectionFactory = jmsConnectionFactory,
            queue = "DEV.QUEUE.1",
        )
    }

    @Test
    fun `skal sende avstemmingsmeldinger på køen`() {
        val utbetalingsoppdrag = listOf(
            utbetalingsoppdrag(id = 1, status = UtbetalingsoppdragStatus.FEILET),
            utbetalingsoppdrag(id = 2, status = UtbetalingsoppdragStatus.FEILET),
            utbetalingsoppdrag(id = 3, status = UtbetalingsoppdragStatus.FEILET)
        )
        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, UUID.randomUUID(), 2)
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val meldingerSendt = avstemmingSender.sendAvstemming(avstemmingsmelding)

        assertEquals(4, meldingerSendt)
    }

    @AfterAll
    fun afterAll() {
        jmsConnectionFactory.stop()
        ibmMQContainer.stop()
    }
}