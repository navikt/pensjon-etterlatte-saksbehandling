package no.nav.etterlatte.behandling.behandlinginfo

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.ktor.runServerWithModule
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.EtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.EtterbetalingPeriodeValg
import no.nav.etterlatte.libs.common.behandling.Feilutbetaling
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.mockedSakTilgangDao
import no.nav.etterlatte.module
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingInfoRoutesTest {
    private val applicationContext: ApplicationContext = mockk(relaxed = true)
    private var behandlingInfoDao: BehandlingInfoDao = mockk()
    private var behandlingService: BehandlingService = mockk()
    private val behandlingsstatusService: BehandlingStatusService = mockk()

    private val server: MockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        server.start()

        behandlingInfoDao = mockk()
        every { applicationContext.tilgangService } returns
            mockk {
                every { harTilgangTilBehandling(any(), any()) } returns true
                every { harTilgangTilSak(any(), any()) } returns true
            }
        every { applicationContext.behandlingInfoService } returns
            BehandlingInfoService(
                behandlingInfoDao,
                behandlingService,
                behandlingsstatusService,
            )
        every { applicationContext.sakTilgangDao } returns mockedSakTilgangDao()

        every { applicationContext.saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn),
            )
    }

    @AfterAll
    fun after() {
        applicationContext.close()
        server.shutdown()
    }

    @Test
    fun `skal lagre brevutfall og etterbetaling`() {
        val behandlingId = UUID.randomUUID()
        val dto = brevutfallOgEtterbetalingDto(behandlingId)

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns brevutfall(behandlingId)
        every { behandlingInfoDao.lagreEtterbetaling(any()) } returns etterbetaling(behandlingId)
        every { behandlingsstatusService.settBeregnet(any(), any(), any()) } returns Unit

        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.post("/api/behandling/${UUID.randomUUID()}/info/brevutfall") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(dto)
                }

            val opprettetBrevutfallOgEtterbetaling: BrevutfallOgEtterbetalingDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            opprettetBrevutfallOgEtterbetaling.brevutfall?.aldersgruppe shouldBe dto.brevutfall?.aldersgruppe
            opprettetBrevutfallOgEtterbetaling.brevutfall?.kilde shouldNotBe null

            opprettetBrevutfallOgEtterbetaling.etterbetaling?.datoFom shouldBe dto.etterbetaling?.datoFom
            opprettetBrevutfallOgEtterbetaling.etterbetaling?.datoTom shouldBe dto.etterbetaling?.datoTom
            opprettetBrevutfallOgEtterbetaling.etterbetaling?.inneholderKrav shouldBe dto.etterbetaling?.inneholderKrav
            opprettetBrevutfallOgEtterbetaling.etterbetaling?.etterbetalingPeriodeValg shouldBe dto.etterbetaling?.etterbetalingPeriodeValg
            opprettetBrevutfallOgEtterbetaling.etterbetaling?.frivilligSkattetrekk shouldBe dto.etterbetaling?.frivilligSkattetrekk
            opprettetBrevutfallOgEtterbetaling.etterbetaling?.kilde shouldNotBe null
        }
    }

    @Test
    fun `skal hente brevutfall`() {
        val behandlingId = UUID.randomUUID()
        val dto = brevutfallOgEtterbetalingDto(behandlingId)

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hentBrevutfall(any()) } returns brevutfall(behandlingId)
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling(behandlingId)

        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.get("/api/behandling/${UUID.randomUUID()}/info/brevutfallogetterbetaling") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentetBrevutfall: BrevutfallOgEtterbetalingDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            hentetBrevutfall.brevutfall?.aldersgruppe shouldBe dto.brevutfall?.aldersgruppe
        }
    }

    @Test
    fun `skal hente etterbetaling`() {
        val behandlingId = UUID.randomUUID()
        val dto = brevutfallOgEtterbetalingDto(behandlingId)

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling(behandlingId)

        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.get("/api/behandling/${UUID.randomUUID()}/info/etterbetaling") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val etterbetaling: EtterbetalingDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            etterbetaling.datoFom shouldBe dto.etterbetaling?.datoFom
            etterbetaling.datoTom shouldBe dto.etterbetaling?.datoTom
            etterbetaling.inneholderKrav shouldBe dto.etterbetaling?.inneholderKrav
            etterbetaling.etterbetalingPeriodeValg shouldBe dto.etterbetaling?.etterbetalingPeriodeValg
            etterbetaling.frivilligSkattetrekk shouldBe dto.etterbetaling?.frivilligSkattetrekk
        }
    }

    private fun behandling(behandlingId: UUID): Behandling =
        mockk {
            every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { id } returns behandlingId
            every { sak } returns
                mockk {
                    every { sakType } returns SakType.BARNEPENSJON
                }
            every { status } returns BehandlingStatus.BEREGNET
            every { revurderingsaarsak() } returns null
            every { virkningstidspunkt } returns
                Virkningstidspunkt.create(
                    YearMonth.of(2023, 1),
                    "begrunnelse",
                    saksbehandler = Grunnlagsopplysning.Saksbehandler.create("ident"),
                )
        }

    private fun brevutfall(behandlingId: UUID) =
        Brevutfall(
            behandlingId = behandlingId,
            aldersgruppe = Aldersgruppe.UNDER_18,
            feilutbetaling = Feilutbetaling(FeilutbetalingValg.NEI, null),
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
        )

    private fun etterbetaling(behandlingId: UUID) =
        Etterbetaling(
            behandlingId = behandlingId,
            fom = YearMonth.of(2023, 1),
            tom = YearMonth.of(2023, 2),
            inneholderKrav = true,
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
            frivilligSkattetrekk = true,
            etterbetalingPeriodeValg = EtterbetalingPeriodeValg.UNDER_3_MND,
        )

    private fun brevutfallOgEtterbetalingDto(behandlingId: UUID = UUID.randomUUID()) =
        BrevutfallOgEtterbetalingDto(
            behandlingId = UUID.randomUUID(),
            opphoer = false,
            brevutfall =
                BrevutfallDto(
                    behandlingId,
                    Aldersgruppe.UNDER_18,
                    Feilutbetaling(FeilutbetalingValg.NEI, null),
                    null,
                ),
            etterbetaling =
                EtterbetalingDto(
                    behandlingId = behandlingId,
                    datoFom = LocalDate.of(2023, 1, 1),
                    datoTom = LocalDate.of(2023, 2, 28),
                    inneholderKrav = true,
                    frivilligSkattetrekk = true,
                    etterbetalingPeriodeValg = EtterbetalingPeriodeValg.UNDER_3_MND,
                    kilde = null,
                ),
        )

    private val token: String by lazy { server.issueSaksbehandlerToken() }
}
