package no.nav.etterlatte.utbetaling.iverksetting.oppdrag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.KvitteringMottaker
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.oppdragMedFeiletKvittering
import no.nav.etterlatte.utbetaling.oppdragMedGodkjentKvittering
import no.nav.etterlatte.utbetaling.utbetaling
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppdragSenderIntegrationTest {

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var jmsConnectionFactory: JmsConnectionFactory
    private lateinit var oppdragSender: OppdragSender
    private lateinit var kvitteringMottaker: KvitteringMottaker
    private val utbetalingService: UtbetalingService = mockk<UtbetalingService>().apply {
        every { oppdaterKvittering(any()) } returns mockk()
        every { oppdaterStatusOgPubliserKvittering(any(), any()) } returns mockk()
    }

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

        kvitteringMottaker = KvitteringMottaker(
            utbetalingService = utbetalingService,
            jmsConnectionFactory = jmsConnectionFactory,
            queue = "DEV.QUEUE.1",
        )
    }

    @AfterAll
    fun afterAll() {
        jmsConnectionFactory.stop()
        ibmMQContainer.stop()
    }

    @Test
    fun `skal sende oppdrag på køen, motta godkjent kvittering og oppdatere status`() {
        val oppdrag = oppdragMedGodkjentKvittering(utbetaling(vedtakId = 1))

        oppdragSender.sendOppdrag(oppdrag)

        verify(timeout = 2000) { utbetalingService.oppdaterKvittering(any()) }
        verify(timeout = 2000) {
            utbetalingService.oppdaterStatusOgPubliserKvittering(
                oppdrag = any(),
                status = UtbetalingStatus.GODKJENT
            )
        }
    }

    @Test
    fun `skal sende oppdrag på køen, motta feilet kvittering og oppdatere status`() {
        val oppdrag = oppdragMedFeiletKvittering(utbetaling(vedtakId = 1))

        oppdragSender.sendOppdrag(oppdrag)

        verify(timeout = 5000) { utbetalingService.oppdaterKvittering(any()) }
        verify(timeout = 5000) {
            utbetalingService.oppdaterStatusOgPubliserKvittering(
                oppdrag = any(),
                status = UtbetalingStatus.FEILET
            )
        }
    }
}