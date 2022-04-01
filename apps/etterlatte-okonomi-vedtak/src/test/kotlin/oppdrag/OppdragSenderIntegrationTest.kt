package no.nav.etterlatte.oppdrag

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.config.JmsConnectionFactoryBuilder
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.oppdragMedFeiletKvittering
import no.nav.etterlatte.oppdragMedGodkjentKvittering
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import javax.jms.Connection

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OppdragSenderIntegrationTest {

    @Container
    private val ibmMQContainer = GenericContainer<Nothing>("ibmcom/mq").apply {
        withEnv("LICENSE","accept")
        withEnv("MQ_QMGR_NAME","QM1")
        withExposedPorts(1414)
    }

    private lateinit var jmsConnection: Connection
    private lateinit var oppdragSender: OppdragSender
    private lateinit var kvitteringMottaker: KvitteringMottaker
    private val utbetalingsoppdragDao: UtbetalingsoppdragDao = mockk<UtbetalingsoppdragDao>().apply {
        every { oppdaterKvittering(any()) } returns mockk()
        every { oppdaterStatus(any(), any()) } returns mockk()
    }

    @BeforeAll
    fun beforeAll() {
        ibmMQContainer.start()

        val jmsConnectionFactory = JmsConnectionFactoryBuilder(
            hostname = ibmMQContainer.host,
            port = ibmMQContainer.firstMappedPort,
            queueManager = "QM1",
            channel = "DEV.ADMIN.SVRCONN",
            username = "admin",
            password = "passw0rd"
        )

        jmsConnection = jmsConnectionFactory.connection()
            .also { it.start() }

        oppdragSender = OppdragSender(
            jmsConnection = jmsConnection,
            queue = "DEV.QUEUE.1",
            replyQueue = "DEV.QUEUE.1"
        )

        kvitteringMottaker = KvitteringMottaker(
            rapidsConnection = TestRapid(),
            utbetalingsoppdragDao = utbetalingsoppdragDao,
            jmsConnection = jmsConnection,
            queue = "DEV.QUEUE.1",
        )
    }

    @AfterAll
    fun afterAll() {
        jmsConnection.close()
        ibmMQContainer.stop()
    }

    @Test
    fun `skal sende oppdrag på køen, motta godkjent kvittering og oppdatere status`() {
        val oppdrag = oppdragMedGodkjentKvittering()

        oppdragSender.sendOppdrag(oppdrag)

        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterKvittering(any())}
        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterStatus(
            vedtakId = oppdrag.vedtakId(),
            status = UtbetalingsoppdragStatus.GODKJENT
        ) }

        confirmVerified(utbetalingsoppdragDao)
    }

    @Test
    fun `skal sende oppdrag på køen, motta feilet kvittering og oppdatere status`() {
        val oppdrag = oppdragMedFeiletKvittering()

        oppdragSender.sendOppdrag(oppdrag)

        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterKvittering(any())}
        verify(timeout = 2000) { utbetalingsoppdragDao.oppdaterStatus(
            vedtakId = oppdrag.vedtakId(),
            status = UtbetalingsoppdragStatus.FEILET
        ) }

        confirmVerified(utbetalingsoppdragDao)
    }
}