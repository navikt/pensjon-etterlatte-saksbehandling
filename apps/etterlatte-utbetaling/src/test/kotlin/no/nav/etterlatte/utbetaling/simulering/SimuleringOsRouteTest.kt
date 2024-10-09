package no.nav.etterlatte.utbetaling.simulering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.toUUID30
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.utbetaling.BehandlingKlient
import no.nav.etterlatte.utbetaling.VedtaksvurderingKlient
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingDao
import no.nav.etterlatte.utbetaling.utbetalingRoutes
import no.nav.etterlatte.utbetaling.vedtak
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.entiteter.infomelding.Infomelding
import no.nav.system.os.entiteter.typer.simpletypes.FradragTillegg
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Month
import java.time.Month.APRIL
import java.time.Month.FEBRUARY
import java.time.YearMonth.of
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimuleringOsRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val utbetalingDao: UtbetalingDao = mockk()
    private val vedtaksvurderingKlient: VedtaksvurderingKlient = mockk()
    private val simuleringDao: SimuleringDao = mockk()
    private val simuleringOsKlient: SimuleringOsKlient = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val simuleringOsService: SimuleringOsService =
        SimuleringOsService(
            utbetalingDao,
            vedtaksvurderingKlient,
            simuleringDao,
            simuleringOsKlient,
        )

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `mappe informasjon fra revurderingsvedtak til simuleringsinput`() {
        val sakId = SakId(1000223L)
        val behandlingId = UUID.randomUUID()

        val utbetalingsperiodeFeb2024 =
            Utbetalingsperiode(
                id = 444L,
                periode = Periode(of(2024, FEBRUARY), of(2024, APRIL)),
                beloep = BigDecimal(2500),
                type = UtbetalingsperiodeType.UTBETALING,
                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
            )
        val utbetalingsperiodeMai2024 =
            Utbetalingsperiode(
                id = 445L,
                periode = Periode(of(2024, Month.MAY), null),
                beloep = BigDecimal(3233),
                type = UtbetalingsperiodeType.UTBETALING,
                regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
            )
        val vedtak =
            vedtak(
                vedtakId = 1,
                saktype = SakType.BARNEPENSJON,
                sakId =
                    no.nav.etterlatte.libs.common.sak
                        .SakId(sakId.value),
                ident = SOEKER_FOEDSELSNUMMER.value,
                virkningstidspunkt = of(2024, FEBRUARY),
                utbetalingsperioder = listOf(utbetalingsperiodeFeb2024, utbetalingsperiodeMai2024),
            )

        coEvery { utbetalingDao.hentUtbetalinger(sakId) } returns emptyList()
        coEvery { simuleringDao.lagre(behandlingId, any(), vedtak, any(), any()) } returns Unit
        coEvery { vedtaksvurderingKlient.hentVedtakSimulering(behandlingId, any()) } returns vedtak
        coEvery {
            behandlingKlient.harTilgangTilBehandling(behandlingId, true, bruker = any())
        } returns true
        coEvery { simuleringOsKlient.simuler(any()) } returns
            SimulerBeregningResponse().apply {
                simulering =
                    Beregning().apply {
                        gjelderId = SOEKER_FOEDSELSNUMMER.value
                        datoBeregnet = "2024-05-02"
                    }
                infomelding =
                    Infomelding().apply {
                        beskrMelding = "Simulering OK"
                    }
            }

        testApplication {
            runServer(mockOAuth2Server) {
                utbetalingRoutes(simuleringOsService, behandlingKlient)
            }

            val response =
                client.post("/api/utbetaling/behandling/$behandlingId/simulering") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            response.status shouldBe HttpStatusCode.OK

            val mappedRequest = slot<SimulerBeregningRequest>()
            coVerify { simuleringOsKlient.simuler(capture(mappedRequest)) }

            with(mappedRequest.captured.oppdrag) {
                fagsystemId shouldBe "1000223"
                oppdragGjelderId shouldBe SOEKER_FOEDSELSNUMMER.value
                saksbehId shouldBe "Z991122"
                utbetFrekvens shouldBe "MND"
                kodeEndring shouldBe "NY"
                kodeFagomraade shouldBe "BARNEPE"
                datoOppdragGjelderFom shouldBe "2024-02-01"
                oppdragslinje shouldHaveSize 2

                // Verifiser verdier uavhengig av periode
                oppdragslinje.forEach {
                    it.vedtakId shouldBe vedtak.id.toString()
                    it.henvisning shouldBe vedtak.behandlingId.toUUID30().value
                    it.utbetalesTilId shouldBe vedtak.sak.ident
                    it.saksbehId shouldBe "Z991122"
                    it.kodeEndringLinje shouldBe "NY"
                    it.kodeKlassifik shouldBe "BARNEPENSJON-OPTP"
                    it.typeSats shouldBe "MND"
                    it.fradragTillegg shouldBe FradragTillegg.T
                    it.brukKjoreplan shouldBe "N"
                }

                with(oppdragslinje[0]) {
                    delytelseId shouldBe utbetalingsperiodeFeb2024.id.toString()
                    datoVedtakFom shouldBe "2024-02-01"
                    datoVedtakTom shouldBe "2024-04-30"
                    sats shouldBe utbetalingsperiodeFeb2024.beloep
                }

                with(oppdragslinje[1]) {
                    delytelseId shouldBe utbetalingsperiodeMai2024.id.toString()
                    datoVedtakFom shouldBe "2024-05-01"
                    datoVedtakTom shouldBe null
                    sats shouldBe utbetalingsperiodeMai2024.beloep
                }
            }
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken(navIdent = "Z991122") }
}
