package no.nav.etterlatte

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.mq.DummyJmsConnectionFactory
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.utbetaling.DatabaseExtension
import no.nav.etterlatte.utbetaling.attestertvedtakEvent
import no.nav.etterlatte.utbetaling.common.UTBETALING_RESPONSE
import no.nav.etterlatte.utbetaling.common.UtbetalingEventDto
import no.nav.etterlatte.utbetaling.common.UtbetalinghendelseType
import no.nav.etterlatte.utbetaling.config.ApplicationContext
import no.nav.etterlatte.utbetaling.config.ApplicationProperties
import no.nav.etterlatte.utbetaling.iverksetting.oppdrag.OppdragJaxb
import no.nav.etterlatte.utbetaling.oppdragMedFeiletKvittering
import no.nav.etterlatte.utbetaling.oppdragMedGodkjentKvittering
import no.nav.etterlatte.utbetaling.ugyldigVedtakTilUtbetaling
import no.nav.etterlatte.utbetaling.vedtak
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationIntegrationTest {
    private val rapidsConnection: TestRapid = spyk(TestRapid())
    private val connectionFactory: EtterlatteJmsConnectionFactory = DummyJmsConnectionFactory()

    @BeforeAll
    fun beforeAll() {
        val props = dbExtension.properties()

        val applicationProperties =
            ApplicationProperties(
                dbName = props.databaseName,
                dbHost = props.host,
                dbPort = props.firstMappedPort,
                dbUsername = props.username,
                dbPassword = props.password,
                mqHost = "mqHost",
                mqPort = -1,
                mqQueueManager = "QM1",
                mqChannel = "DEV.ADMIN.SVRCONN",
                mqSendQueue = "DEV.QUEUE.1",
                mqKvitteringQueue = "DEV.QUEUE.2",
                mqAvstemmingQueue = "DEV.QUEUE.1",
                serviceUserUsername = "admin",
                serviceUserPassword = "passw0rd",
                leaderElectorPath = "",
                grensesnittavstemmingEnabled = false,
                konsistensavstemmingEnabled = false,
                grensesnittavstemmingOMSEnabled = false,
                konsistensavstemmingOMSEnabled = false,
            )

        ApplicationContext(
            env = Miljoevariabler.systemEnv(),
            properties = applicationProperties,
            jmsConnectionFactory = connectionFactory,
            behandlingKlient = mockk(),
            vedtaksvurderingKlient = mockk(),
            simuleringOsKlient = mockk(),
        ).also {
            rapidsConnection.settOppRiversOgListener(it)
            rapidsConnection.start()
        }
    }

    @Test
    fun `skal sende utbetaling til oppdrag`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(
                    behandling =
                        Behandling(
                            id = behandlingId,
                            type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        ),
                ),
            ),
        )

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                            this.utbetalingResponse.vedtakId == 1L &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.SENDT &&
                            this.utbetalingResponse.behandlingId == behandlingId
                    }
                },
            )
        }
    }

    @Test
    fun `skal feile dersom vedtak ikke kan leses`() {
        sendFattetVedtakEvent(attestertvedtakEvent(ugyldigVedtakTilUtbetaling()))

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding
                                ?.contains(
                                    "En feil oppstod under prosessering av vedtak med vedtakId=null",
                                ) != false &&
                            this.utbetalingResponse.behandlingId == null
                    }
                },
            )
        }
    }

    @Test
    fun `skal feile dersom det finnes utbetaling for vedtaket allerede`() {
        val behandlingIdForste = UUID.randomUUID()
        val behandlingIdAndre = UUID.randomUUID()
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(
                    behandling =
                        Behandling(
                            id = behandlingIdForste,
                            type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        ),
                ),
            ),
        )
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(
                    behandling =
                        Behandling(
                            id = behandlingIdAndre,
                            type = BehandlingType.FØRSTEGANGSBEHANDLING,
                        ),
                ),
            ),
        )

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding
                                ?.contains("Vedtak med vedtakId=1 eksisterer fra før") != false &&
                            this.utbetalingResponse.feilmelding
                                ?.contains("behandlingId for nytt vedtak: $behandlingIdAndre") == true &&
                            this.utbetalingResponse.feilmelding
                                ?.contains("behandlingId for tidligere utbetaling: $behandlingIdForste") == true &&
                            this.utbetalingResponse.behandlingId == behandlingIdAndre
                    }
                },
            )
        }
    }

    @Test
    fun `skal feile dersom det eksisterer utbetalingslinjer med samme id som i nytt vedtak`() {
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(),
            ),
        )
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(
                    vedtakId = 2,
                    behandling =
                        Behandling(
                            BehandlingType.FØRSTEGANGSBEHANDLING,
                            behandlingId,
                        ),
                ),
            ),
        )

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                match {
                    objectMapper.readValue(it, UtbetalingEventDto::class.java).run {
                        this.eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                            this.utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                            this.utbetalingResponse.feilmelding
                                ?.contains(
                                    "En eller flere utbetalingslinjer med id=[1] eksisterer fra før",
                                ) != false &&
                            this.utbetalingResponse.behandlingId == behandlingId
                    }
                },
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag som er godkjent`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(
                    behandling =
                        Behandling(
                            BehandlingType.FØRSTEGANGSBEHANDLING,
                            behandlingId,
                        ),
                ),
            ),
        )
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                any(),
                match {
                    val (eventName, utbetalingResponse) = it.toResponse()

                    return@match eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                        utbetalingResponse.vedtakId == 1L &&
                        utbetalingResponse.status == UtbetalingStatusDto.GODKJENT &&
                        utbetalingResponse.behandlingId == behandlingId
                },
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
                    val (eventName, utbetalingResponse) = it.toResponse()

                    return@match eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                        utbetalingResponse.vedtakId == 1L &&
                        utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                        utbetalingResponse.behandlingId == null
                },
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag som er godkjent men feiler fordi status for utbetaling er ugyldig`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(
                    behandling =
                        Behandling(
                            BehandlingType.FØRSTEGANGSBEHANDLING,
                            behandlingId,
                        ),
                ),
            ),
        )
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering()) // setter status til GODKJENT
        simulerKvitteringsmeldingFraOppdrag(oppdragMedGodkjentKvittering()) // forventer at status skal være SENDT

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                any(),
                match {
                    val (eventName, utbetalingResponse) = it.toResponse()

                    return@match eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                        utbetalingResponse.vedtakId == 1L &&
                        utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                        utbetalingResponse.feilmelding ==
                        "Utbetalingen for vedtakId=1 har feil status (GODKJENT)" &&
                        utbetalingResponse.behandlingId == behandlingId
                },
            )
        }
    }

    @Test
    fun `skal motta kvittering fra oppdrag som har feilet`() {
        val behandlingId = UUID.randomUUID()
        sendFattetVedtakEvent(
            attestertvedtakEvent(
                vedtak(
                    behandling =
                        Behandling(
                            BehandlingType.FØRSTEGANGSBEHANDLING,
                            behandlingId,
                        ),
                ),
            ),
        )
        simulerKvitteringsmeldingFraOppdrag(oppdragMedFeiletKvittering())

        verify(timeout = TIMEOUT) {
            rapidsConnection.publish(
                any(),
                match {
                    val (eventName, utbetalingResponse) = it.toResponse()

                    return@match eventName == UtbetalinghendelseType.OPPDATERT.lagEventnameForType() &&
                        utbetalingResponse.vedtakId == 1L &&
                        utbetalingResponse.status == UtbetalingStatusDto.FEILET &&
                        utbetalingResponse.feilmelding == "KodeMelding Beskrivelse" &&
                        utbetalingResponse.behandlingId == behandlingId
                },
            )
        }
    }

    @AfterEach
    fun afterEach() {
        rapidsConnection.reset()
        dbExtension.resetDb()
    }

    @AfterAll
    fun afterAll() {
        rapidsConnection.stop()
    }

    private fun sendFattetVedtakEvent(vedtakEvent: String) {
        rapidsConnection.sendTestMessage(vedtakEvent)
    }

    private fun simulerKvitteringsmeldingFraOppdrag(oppdrag: Oppdrag) {
        connectionFactory.send(queue = "DEV.QUEUE.2", xml = OppdragJaxb.toXml(oppdrag))
    }

    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()

        const val TIMEOUT: Long = 5000
    }

    private fun String.toResponse(): Pair<String, UtbetalingResponseDto> {
        val message = objectMapper.readTree(this)

        return Pair(
            message[EVENT_NAME_KEY].asText(),
            objectMapper.treeToValue(
                message[UTBETALING_RESPONSE],
            ),
        )
    }
}
