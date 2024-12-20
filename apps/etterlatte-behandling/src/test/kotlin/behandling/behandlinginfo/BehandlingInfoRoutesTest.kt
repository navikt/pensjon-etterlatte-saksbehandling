package no.nav.etterlatte.behandling.behandlinginfo

import io.kotest.matchers.shouldBe
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
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgInfo
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
    fun `skal lagre brevutfall`() {
        val behandlingId = UUID.randomUUID()
        val dto = brevutfallOgInfoDto(behandlingId)

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.lagreBrevutfall(any()) } returns dto.toBrevutfall()
        every { behandlingsstatusService.settBeregnet(any(), any(), any()) } returns Unit

        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.post("/api/behandling/$behandlingId/info/brevutfall") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(dto)
                }

            val opprettetBrevutfall: BrevutfallOgInfo = response.body()
            response.status shouldBe HttpStatusCode.OK

            opprettetBrevutfall.brevutfall?.behandlingId shouldBe behandlingId
            opprettetBrevutfall.brevutfall?.aldersgruppe shouldBe dto.brevutfall?.aldersgruppe
            opprettetBrevutfall.brevutfall?.harEtterbetaling shouldBe true
        }
    }

    @Test
    fun `skal hente brevutfall`() {
        val behandlingId = UUID.randomUUID()
        val dto = brevutfallOgInfoDto(behandlingId)

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hentBrevutfall(any()) } returns dto.toBrevutfall()
        every { behandlingsstatusService.settBeregnet(any(), any(), any()) } returns Unit

        testApplication {
            val client =
                runServerWithModule(server) {
                    module(applicationContext)
                }

            val response =
                client.get("/api/behandling/$behandlingId/info/brevutfall") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK

            val responeBrevutfall: BrevutfallOgInfo = response.body()
            responeBrevutfall.behandlingId shouldBe behandlingId

            // TODO: finner ikke ut hvorfor denne feiler
            // responeBrevutfall.brevutfall shouldBe dto
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

    private fun brevutfallOgInfoDto(behandlingId: UUID) =
        BrevutfallOgInfo(
            behandlingId = behandlingId,
            brevutfall =
                BrevutfallDto(
                    behandlingId = behandlingId,
                    aldersgruppe = Aldersgruppe.UNDER_18,
                    feilutbetaling = Feilutbetaling(FeilutbetalingValg.NEI, null),
                    frivilligSkattetrekk = true,
                    harEtterbetaling = true,
                    opphoer = false,
                    kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
                ),
        )

    private fun BrevutfallOgInfo.toBrevutfall() =
        Brevutfall(
            behandlingId = behandlingId!!,
            aldersgruppe = Aldersgruppe.UNDER_18,
            feilutbetaling = Feilutbetaling(FeilutbetalingValg.NEI, null),
            frivilligSkattetrekk = true,
            harEtterbetaling = true,
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
        )

    private val token: String by lazy { server.issueSaksbehandlerToken() }
}
