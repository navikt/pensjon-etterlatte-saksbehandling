package no.nav.etterlatte.oppdrag

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.oppdragMedFeiletKvittering
import no.nav.etterlatte.oppdragMedGodkjentKvittering
import no.nav.etterlatte.util.TestContainers
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppdragSenderIntegrationTest {

    @Container private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var jmsConnectionFactory: JmsConnectionFactory
    private lateinit var oppdragSender: OppdragSender
    private lateinit var kvitteringMottaker: KvitteringMottaker
    private val utbetalingsoppdragDao: UtbetalingsoppdragDao = mockk<UtbetalingsoppdragDao>().apply {
        every { oppdaterKvittering(any()) } returns mockk()
        every { oppdaterStatus(any(), any()) } returns mockk()
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
            rapidsConnection = TestRapid(),
            utbetalingsoppdragDao = utbetalingsoppdragDao,
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
        val oppdrag = oppdragMedGodkjentKvittering(vedtakId = "1")

        oppdragSender.sendOppdrag(oppdrag)

        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterKvittering(any())}
        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterStatus(
            vedtakId = oppdrag.vedtakId(),
            status = UtbetalingsoppdragStatus.GODKJENT
        ) }
    }

    @Test
    fun `skal sende oppdrag på køen, motta feilet kvittering og oppdatere status`() {
        val oppdrag = oppdragMedFeiletKvittering(vedtakId = "2")

        oppdragSender.sendOppdrag(oppdrag)

        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterKvittering(any())}
        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterStatus(
            vedtakId = oppdrag.vedtakId(),
            status = UtbetalingsoppdragStatus.FEILET
        ) }
    }
}