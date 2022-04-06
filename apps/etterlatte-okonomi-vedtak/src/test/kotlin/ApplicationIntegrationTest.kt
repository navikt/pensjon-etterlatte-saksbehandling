package no.nav.etterlatte

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.common.Jaxb
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.etterlatte.util.TestContainers
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

    @Container private val postgreSQLContainer = TestContainers.postgreSQLContainer
    @Container private val ibmMQContainer = TestContainers.ibmMQContainer

    private lateinit var rapidsConnection: TestRapid
    private lateinit var connectionFactory: JmsConnectionFactory

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ibmMQContainer.start()

        val env = mapOf(
            "DB_HOST" to postgreSQLContainer.host,
            "DB_PORT" to postgreSQLContainer.firstMappedPort.toString(),
            "DB_DATABASE" to postgreSQLContainer.databaseName,
            "DB_USERNAME" to postgreSQLContainer.username,
            "DB_PASSWORD" to postgreSQLContainer.password,

            "OPPDRAG_SEND_MQ_NAME" to "DEV.QUEUE.1",
            "OPPDRAG_KVITTERING_MQ_NAME" to "DEV.QUEUE.2",
            "OPPDRAG_MQ_HOSTNAME" to ibmMQContainer.host,
            "OPPDRAG_MQ_PORT" to ibmMQContainer.firstMappedPort.toString(),
            "OPPDRAG_MQ_CHANNEL" to "DEV.ADMIN.SVRCONN",
            "OPPDRAG_MQ_MANAGER" to "QM1",

            "srvuser" to "admin",
            "srvpwd" to "passw0rd",
        )

        val applicationContext = spyk(ApplicationContext(env)).apply {
            every { rapidsConnection() } returns spyk(TestRapid()).also { rapidsConnection = it }
            every { jmsConnectionFactory() } answers { spyk(callOriginal()).also { connectionFactory = it } }
        }

        rapidApplication(applicationContext).start()
    }

    @AfterAll
    fun afterAll() {
        connectionFactory.stop()
        ibmMQContainer.stop()
        postgreSQLContainer.stop()
        rapidsConnection.stop()
    }

    @AfterEach
    fun afterEach() {
        rapidsConnection.reset()
    }

    @Test
    fun `skal motta vedtak, lagre i db og sende til MQ - motta kvittering og sende oppdrag_godkjent event`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        verify(timeout = TIMEOUT) { rapidsConnection.publish( match { it.contains("sendt_til_utbetaling") }) }

        sendKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering(vedtakId = "1"))
        verify(timeout = TIMEOUT) { rapidsConnection.publish( match { it.contains("utbetaling_godkjent") }) }
    }

    @Test
    fun `skal motta vedtak, lagre i db og sende til MQ - motta kvittering og sende oppdrag_feilet event`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_2)
        verify(timeout = TIMEOUT) { rapidsConnection.publish( match { it.contains("sendt_til_utbetaling") }) }

        sendKvitteringsmeldingFraOppdrag(oppdragMedFeiletKvittering(vedtakId = "2"))
        verify(timeout = TIMEOUT) { rapidsConnection.publish( match { it.contains("utbetaling_feilet") }) }
    }

    private fun sendFattetVedtakEvent(vedtakEvent: String) {
        rapidsConnection.sendTestMessage(vedtakEvent)
    }

    private fun sendKvitteringsmeldingFraOppdrag(oppdrag: Oppdrag) {
        connectionFactory.connection().createSession().use { session ->
            val producer = session.createProducer(session.createQueue("DEV.QUEUE.2"))
            val message = session.createTextMessage(Jaxb.toXml(oppdrag))
            producer.send(message)
        }
    }

    companion object {
        val FATTET_VEDTAK_1 = readFile("/vedtak1.json")
        val FATTET_VEDTAK_2 = readFile("/vedtak2.json")
        const val TIMEOUT: Long = 2000
    }
}