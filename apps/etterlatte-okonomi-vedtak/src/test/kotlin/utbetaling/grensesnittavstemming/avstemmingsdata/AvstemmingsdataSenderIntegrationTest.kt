package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetalingsoppdrag
import no.nav.su.se.bakover.common.Tidspunkt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.temporal.ChronoUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvstemmingsdataSenderIntegrationTest {

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer
    private lateinit var jmsConnectionFactory: JmsConnectionFactory
    private lateinit var avstemmingsdataSender: AvstemmingsdataSender

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

        avstemmingsdataSender = AvstemmingsdataSender(
            jmsConnectionFactory = jmsConnectionFactory,
            queue = "DEV.QUEUE.1",
        )
    }

    @Test
    fun `skal sende avstemmingsmeldinger på køen`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalingsoppdrag = listOf(
            utbetalingsoppdrag(id = 1, status = UtbetalingStatus.FEILET),
            utbetalingsoppdrag(id = 2, status = UtbetalingStatus.FEILET),
            utbetalingsoppdrag(id = 3, status = UtbetalingStatus.FEILET)
        )
        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalingsoppdrag, fraOgMed, til, "1", 2)
        val avstemmingsmelding = avstemmingsdataMapper.opprettAvstemmingsmelding()

        avstemmingsdataSender.sendAvstemming(avstemmingsmelding.first())
    }

    @AfterAll
    fun afterAll() {
        jmsConnectionFactory.stop()
        ibmMQContainer.stop()
    }
}