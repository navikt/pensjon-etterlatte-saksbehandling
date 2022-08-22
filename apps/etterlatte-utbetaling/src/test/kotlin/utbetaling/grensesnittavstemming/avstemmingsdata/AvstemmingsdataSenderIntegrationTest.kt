package no.nav.etterlatte.utbetaling.grensesnittavstemming.avstemmingsdata

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.grensesnittavstemming.UUIDBase64
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.utbetaling
import no.nav.etterlatte.utbetaling.utbetalingshendelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
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
            queue = "DEV.QUEUE.1"
        )
    }

    @Test
    fun `skal sende avstemmingsmeldinger på koeen`() {
        val fraOgMed = Tidspunkt(Instant.now().minus(1, ChronoUnit.DAYS))
        val til = Tidspunkt.now()

        val utbetalinger = listOf(
            utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET))),
            utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET))),
            utbetaling(utbetalingshendelser = listOf(utbetalingshendelse(status = UtbetalingStatus.FEILET)))
        )
        val avstemmingsdataMapper = AvstemmingsdataMapper(utbetalinger, fraOgMed, til, UUIDBase64(), 2)
        val avstemmingsdata = avstemmingsdataMapper.opprettAvstemmingsmelding()

        val xml = avstemmingsdataSender.sendAvstemming(avstemmingsdata.first())

        assertNotNull(xml)
    }

    @AfterAll
    fun afterAll() {
        jmsConnectionFactory.stop()
        ibmMQContainer.stop()
    }
}