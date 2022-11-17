package no.nav.etterlatte

import io.mockk.spyk
import io.mockk.verify
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.utbetaling.EVENT_NAME_OPPDATERT
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingEventDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.utbetaling.TestContainers
import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.etterlatte.utbetaling.config.ApplicationProperties
import no.nav.etterlatte.utbetaling.config.JmsConnectionFactory
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.oppdragMedFeiletKvittering
import no.nav.etterlatte.utbetaling.oppdragMedGodkjentKvittering
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
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {

    @Container
    private val postgreSQLContainer = TestContainers.postgreSQLContainer

    @Container
    private val ibmMQContainer = TestContainers.ibmMQContainer

    private val rapidsConnection: TestRapid = spyk(TestRapid())
    private lateinit var connectionFactory: JmsConnectionFactory
    private lateinit var dataSource: DataSource

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
            leaderElectorPath = ""
        )

        ApplicationContext(applicationProperties, rapidsConnection).also {
            connectionFactory = it.jmsConnectionFactory
            dataSource = it.dataSource
            rapidApplication(it).start()
        }
    }

    @Test
    fun `skal sende utbetaling til oppdrag`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak(
                    behandling = Behandling(
                        id = behandlingId,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING
                    )
                )
            )
        )

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.vedtakId == 1L &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.SENDT &&
                            this.utbetalingResponse.behandlingId == behandlingId
                    }
                }
            )
        }
    }

    @Test
    fun `skal feile dersom vedtak ikke kan leses`() {
        sendFattetVedtakEvent(vedtakEvent(ugyldigVedtakTilUtbetaling()))

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding
                            ?.contains(
                                "En feil oppstod under prosessering av vedtak med vedtakId=null"
                            ) != false &&
                            this.utbetalingResponse.behandlingId == null
                    }
                }
            )
        }
    }

    @Test
    fun `skal feile dersom det finnes utbetaling for vedtaket allerede`() {
        val behandlingId_forste = UUID.randomUUID()
        val behandlingId_andre = UUID.randomUUID()
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak(
                    behandling = Behandling(
                        id = behandlingId_forste,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING
                    )
                )
            )
        )
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak(
                    behandling = Behandling(
                        id = behandlingId_andre,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING
                    )
                )
            )
        )

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding
                            ?.contains("Vedtak med vedtakId=1 eksisterer fra før") != false &&
                            this.utbetalingResponse.feilmelding
                            ?.contains("behandlingId for nytt vedtak: $behandlingId_andre") == true &&
                            this.utbetalingResponse.feilmelding
                            ?.contains("behandlingId for tidligere utbetaling: $behandlingId_forste") == true &&
                            this.utbetalingResponse.behandlingId == behandlingId_andre
                    }
                }
            )
        }
    }

    @Test
    fun `skal feile dersom det eksisterer utbetalingslinjer med samme id som i nytt vedtak`() {
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak()
            )
        )
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak(
                    vedtakId = 2,
                    behandling = Behandling(
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        behandlingId
                    )
                )
            )
        )

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding
                            ?.contains(
                                "En eller flere utbetalingslinjer med id=[1] eksisterer fra før"
                            ) != false &&
                            this.utbetalingResponse.behandlingId == behandlingId
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag som er godkjent`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak(
                    behandling = Behandling(
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        behandlingId
                    )
                )
            )
        )
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                any(),
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.vedtakId == 1L &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.GODKJENT &&
                            this.utbetalingResponse.behandlingId == behandlingId
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag som er godkjent men feiler fordi utbetaling for vedtak ikke finnes`() {
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                any(),
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.vedtakId == 1L &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.behandlingId == null
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag som er godkjent men feiler fordi status for utbetaling er ugyldig`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak(
                    behandling = Behandling(
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        behandlingId
                    )
                )
            )
        )
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering()) // setter status til GODKJENT
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering()) // forventer at status skal være SENDT

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                any(),
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.vedtakId == 1L &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding ==
                            "Utbetalingen for vedtakId=1 har feil status (GODKJENT)" &&
                            this.utbetalingResponse.behandlingId == behandlingId
                    }
                }
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag som har feilet`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            vedtakEvent(
                vedtak(
                    behandling = Behandling(
                        BehandlingType.FØRSTEGANGSBEHANDLING,
                        behandlingId
                    )
                )
            )
        )
        simulerKvitteringsmeldingFraOppdrag(oppdragMedFeiletKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                any(),
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.event == EVENT_NAME_OPPDATERT &&
                            this.utbetalingResponse.vedtakId == 1L &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding == "KodeMelding Beskrivelse" &&
                            this.utbetalingResponse.behandlingId == behandlingId
                    }
                }
            )
        }
    }

    @AfterEach
    fun afterEach() {
        rapidsConnection.reset()

        using(sessionOf(dataSource)) {
            it.run(queryOf("TRUNCATE utbetaling CASCADE").asExecute)
        }
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
        const val TIMEOUT: Long = 5000
    }
}