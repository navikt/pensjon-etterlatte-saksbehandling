package no.nav.etterlatte

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.ktor.http.Url
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.etterlatte.utbetaling.config.ApplicationProperties
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.oppdragMedFeiletKvittering
import no.nav.etterlatte.utbetaling.oppdragMedGodkjentKvittering
import no.nav.etterlatte.utbetaling.readFile
import no.nav.etterlatte.utbetaling.ugyldigVedtakTilUtbetaling
import no.nav.etterlatte.utbetaling.vedtak
import no.nav.etterlatte.utbetaling.vedtakEvent
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

    private val rapidsConnection: TestRapid = spyk(TestRapid())
    private lateinit var connectionFactory: JmsConnectionFactory

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ibmMQContainer.start()
        electionServer.start()

        val applicationProperties = ApplicationProperties(
            dbName = postgreSQLContainer.databaseName,
            dbHost = postgreSQLContainer.host,
            dbPort = postgreSQLContainer.firstMappedPort,
            dbUsername = postgreSQLContainer.username,
            dbPassword = postgreSQLContainer.password,
            mqHost = ibmMQContainer.host,
            mqPort = ibmMQContainer.firstMappedPort,
            mqQueueManager = "QM1",
            mqChannel = "DEV.ADMIN.SVRCONN",
            mqSendQueue = "DEV.QUEUE.1",
            mqKvitteringQueue = "DEV.QUEUE.2",
            mqAvstemmingQueue = "DEV.QUEUE.1",
            serviceUserUsername = "admin",
            serviceUserPassword = "passw0rd",
            leaderElectorPath = Url(electionServer.baseUrl()).let { "${it.host}:${it.port}" },
        )

        electionServer.stubFor(
            get(urlEqualTo("/"))
                .willReturn(
                    aResponse()
                        .withBody(mapOf("name" to "some.value").toJson())
                )
        )

        ApplicationContext(applicationProperties, rapidsConnection).also {
            connectionFactory = it.jmsConnectionFactory
            it.dataSourceBuilder.migrate() // TODO bør ikke trenge denne med TestRapid()

            rapidApplication(it).start()
        }
    }

    @Test
    fun `skal sende utbetaling til oppdrag`() {
        sendFattetVedtakEvent(vedtakEvent(vedtak()))

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@vedtakId"].longValue() == 1L &&
                                event["@status"].textValue() == UtbetalingStatus.SENDT.name
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        sendKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@vedtakId"].longValue() == 1L &&
                                event["@status"].textValue() == UtbetalingStatus.GODKJENT.name
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag med feil`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        sendKvitteringsmeldingFraOppdrag(oppdragMedFeiletKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "utbetaling_oppdatert" &&
                                event["@vedtakId"].longValue() == 1L &&
                                event["@status"].textValue() == UtbetalingStatus.FEILET.name
                    }
                }
            )
        }
    }

    @Test
    fun `skal post melding paa kafka dersom vedtak ikke kan deserialiseres`() {
        sendFattetVedtakEvent(vedtakEvent(ugyldigVedtakTilUtbetaling()))

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    it.toJsonNode().let { event ->
                        event["@event_name"].textValue() == "deserialisering_feilet"
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
        val FATTET_VEDTAK_1 = readFile("/vedtak.json")
        const val TIMEOUT: Long = 5000
    }
}