package no.nav.etterlatte

import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.etterlatte.utbetaling.config.ApplicationProperties
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.utbetaling.iverksetting.UtbetalingEvent
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingStatus
import no.nav.etterlatte.utbetaling.oppdragMedFeiletKvittering
import no.nav.etterlatte.utbetaling.oppdragMedGodkjentKvittering
import no.nav.etterlatte.utbetaling.readFile
import no.nav.etterlatte.utbetaling.ugyldigVedtakTilUtbetaling
import no.nav.etterlatte.utbetaling.vedtak
import no.nav.etterlatte.utbetaling.vedtakEvent
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

    private val rapidsConnection: TestRapid = spyk(TestRapid())
    private lateinit var connectionFactory: JmsConnectionFactory

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        ibmMQContainer.start()

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
            leaderElectorPath = "",
        )

        ApplicationContext(applicationProperties, rapidsConnection).also {
            connectionFactory = it.jmsConnectionFactory
            rapidApplication(it).start()
        }
    }

    @Test
    fun `skal sende utbetaling til oppdrag`() {
        sendFattetVedtakEvent(vedtakEvent(vedtak()))

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    objectMapper.readValue(it, UtbetalingEvent::class.java).run {
                        this.eventName == EVENT_NAME_OPPDATERT &&
                                this.utbetalingResponse.vedtakId == 1L &&
                                this.utbetalingResponse.status == UtbetalingStatus.SENDT
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    objectMapper.readValue(it, UtbetalingEvent::class.java).run {
                        this.eventName == EVENT_NAME_OPPDATERT &&
                                this.utbetalingResponse.vedtakId == 1L &&
                                this.utbetalingResponse.status == UtbetalingStatus.GODKJENT
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag med feil`() {
        sendFattetVedtakEvent(FATTET_VEDTAK_1)
        simulerKvitteringsmeldingFraOppdrag(oppdragMedFeiletKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish("key",
                match {
                    objectMapper.readValue(it, UtbetalingEvent::class.java).run {
                        this.eventName == EVENT_NAME_OPPDATERT &&
                                this.utbetalingResponse.vedtakId == 1L &&
                                this.utbetalingResponse.status == UtbetalingStatus.FEILET &&
                                this.utbetalingResponse.feilmelding == "KodeMelding Beskrivelse"
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
                    objectMapper.readValue(it, UtbetalingEvent::class.java).run {
                        this.eventName == EVENT_NAME_OPPDATERT &&
                                this.utbetalingResponse.status == UtbetalingStatus.FEILET &&
                                this.utbetalingResponse.feilmelding
                                    ?.contains("En feil oppstod under prosessering av vedtak med vedtakId=null") != null
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
        rapidsConnection.stop()
        ibmMQContainer.stop()
        postgreSQLContainer.stop()
    }

    private fun sendFattetVedtakEvent(vedtakEvent: String) {
        rapidsConnection.sendTestMessage(vedtakEvent)
    }

    private fun simulerKvitteringsmeldingFraOppdrag(oppdrag: Oppdrag) {
        connectionFactory.connection().createSession().use { session ->
            val producer = session.createProducer(session.createQueue("DEV.QUEUE.2"))
            val message = session.createTextMessage(OppdragJaxb.toXml(oppdrag))
            producer.send(message)
        }
    }

    companion object {
        val FATTET_VEDTAK_1 = readFile("/vedtak.json")
        const val TIMEOUT: Long = 5000
    }
}