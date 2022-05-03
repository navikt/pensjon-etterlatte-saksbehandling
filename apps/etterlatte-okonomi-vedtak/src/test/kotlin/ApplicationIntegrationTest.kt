package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.etterlatte.avstemming.GrensesnittsavstemmingJob
import no.nav.etterlatte.avstemming.GrensesnittsavstemmingService
import no.nav.etterlatte.common.Jaxb
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.etterlatte.config.LeaderElection
import no.nav.etterlatte.config.required
import no.nav.etterlatte.domain.UtbetalingsoppdragStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.util.TestContainers
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

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
            "OPPDRAG_AVSTEMMING_MQ_NAME" to "DEV.QUEUE.1",

            "srvuser" to "admin",
            "srvpwd" to "passw0rd",

            "ELECTOR_PATH" to "some.path"
        )

        val slot = slot<GrensesnittsavstemmingService>()

        val applicationContext = spyk(ApplicationContext(env)).apply {
            every { rapidsConnection() } returns spyk(TestRapid()).also { rapidsConnection = it }
            every { jmsConnectionFactory() } answers { spyk(callOriginal()).also { connectionFactory = it } }
            every { avstemmingJob(capture(slot), any(), any()) } answers {
                GrensesnittsavstemmingJob(
                    grensesnittsavstemmingService = slot.captured,
                    leaderElection = LeaderElection(env.required("ELECTOR_PATH"), HttpClient(mockElectionResult())),
                    starttidspunkt = Date.from(Instant.now().plusSeconds(5)),
                    periode = Duration.ofSeconds(30)
                )
            }
        }

        rapidApplication(applicationContext).start()
    }

    private fun mockElectionResult() = MockEngine {
        respond(
            content = mapOf("name" to withContext(Dispatchers.IO) {
                InetAddress.getLocalHost()
            }.hostName).toJson(),
            status = HttpStatusCode.OK
        )
    }

    @Test
    fun `test avstemmingsjobb`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
    }

    @Test
    fun `skal sende oppdrag og motta kvittering for godkjent oppdrag`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@status"].textValue() == UtbetalingsoppdragStatus.SENDT.name
                    }
                }
            )
        }

        sendKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering(vedtakId = "1"))
        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    it.toJsonNode().let { event ->
                        event["@status"].textValue() == UtbetalingsoppdragStatus.GODKJENT.name
                    }
                }
            )
        }

    }

    @Test
    fun `skal sende oppdrag og motta kvittering for feilet oppdrag`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_2)
        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@status"].textValue() == UtbetalingsoppdragStatus.SENDT.name
                    }
                }
            )
        }

        sendKvitteringsmeldingFraOppdrag(oppdragMedFeiletKvittering(vedtakId = "2"))
        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@status"].textValue() == UtbetalingsoppdragStatus.FEILET.name
                    }
                }
            )
        }
    }

    @AfterEach
    fun afterEach() {
        rapidsConnection.reset()
    }

    @AfterAll
    fun afterAll() {
        connectionFactory.stop()
        ibmMQContainer.stop()
        postgreSQLContainer.stop()
        rapidsConnection.stop()
    }

    // TODO legg på flere tilsvarende tester her når vi vet hvilke statuser vi får

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

    private fun String.toJsonNode() = objectMapper.readTree(this)

    companion object {
        val FATTET_VEDTAK_1 = readFile("/vedtak1.json")
        val FATTET_VEDTAK_2 = readFile("/vedtak2.json")
        const val TIMEOUT: Long = 10000
    }
}