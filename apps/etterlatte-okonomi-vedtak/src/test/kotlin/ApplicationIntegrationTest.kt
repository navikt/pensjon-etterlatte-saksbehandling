package no.nav.etterlatte

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.config.JmsConnectionFactory
import no.nav.etterlatte.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
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

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

    private val electionServer = WireMockServer(options().port(8089))

    private lateinit var rapidsConnection: TestRapid
    private lateinit var connectionFactory: JmsConnectionFactory

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ibmMQContainer.start()
        electionServer.start()

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

            "ELECTOR_PATH" to electionServer.baseUrl().replace("http://", "")
        )

        electionServer.stubFor(
            get(urlEqualTo("/"))
                .willReturn(
                    aResponse()
                        .withBody(mapOf("name" to "some.value").toJson())
                )
        )

        spyk(ApplicationContext(env)).apply {
            every { rapidsConnection() } returns spyk(TestRapid()).also { rapidsConnection = it }
            every { jmsConnectionFactory() } answers { spyk(callOriginal()).also { connectionFactory = it } }
        }.run { rapidApplication(this).start() }
    }

    @Test
    fun `skal sende utbetaling til oppdrag`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@vedtakId"].textValue() == "1" &&
                                event["@status"].textValue() == UtbetalingStatus.SENDT.name
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        sendKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering(vedtakId = "1"))

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@vedtakId"].textValue() == "1" &&
                                event["@status"].textValue() == UtbetalingStatus.GODKJENT.name
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag med feil`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        sendKvitteringsmeldingFraOppdrag(oppdragMedFeiletKvittering(vedtakId = "1"))

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@vedtakId"].textValue() == "1" &&
                                event["@status"].textValue() == UtbetalingStatus.FEILET.name
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

    private fun sendFattetVedtakEvent(vedtakEvent: String) {
        rapidsConnection.sendTestMessage(vedtakEvent)
    }

    private fun sendKvitteringsmeldingFraOppdrag(oppdrag: Oppdrag) {
        connectionFactory.connection().createSession().use { session ->
            val producer = session.createProducer(session.createQueue("DEV.QUEUE.2"))
            val message = session.createTextMessage(OppdragJaxb.toXml(oppdrag))
            producer.send(message)
        }
    }

    private fun String.toJsonNode() = objectMapper.readTree(this)

    companion object {
        val FATTET_VEDTAK_1 = readFile("/vedtak1.json")
        const val TIMEOUT: Long = 5000
    }
}