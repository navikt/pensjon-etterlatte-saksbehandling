package no.nav.etterlatte.behandling.behandlinginfo

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.EtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.LavEllerIngenInntekt
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.module
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
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
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)

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

        coEvery { applicationContext.navAnsattKlient.hentEnhetForSaksbehandler(any()) } returns
            listOf(
                SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn),
            )
    }

    @AfterAll
    fun after() {
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
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.post("/api/behandling/${UUID.randomUUID()}/info/brevutfall") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(dto)
                }

            val opprettetBrevutfallOgEtterbetaling: BrevutfallOgEtterbetalingDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            opprettetBrevutfallOgEtterbetaling.brevutfall?.aldersgruppe shouldBe dto.brevutfall?.aldersgruppe
            opprettetBrevutfallOgEtterbetaling.brevutfall?.lavEllerIngenInntekt shouldBe dto.brevutfall?.lavEllerIngenInntekt
            opprettetBrevutfallOgEtterbetaling.brevutfall?.kilde shouldNotBe null

            opprettetBrevutfallOgEtterbetaling.etterbetaling?.datoFom shouldBe dto.etterbetaling?.datoFom
            opprettetBrevutfallOgEtterbetaling.etterbetaling?.datoTom shouldBe dto.etterbetaling?.datoTom
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
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.get("/api/behandling/${UUID.randomUUID()}/info/brevutfallogetterbetaling") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentetBrevutfall: BrevutfallOgEtterbetalingDto = response.body()
            println(response.bodyAsText())
            response.status shouldBe HttpStatusCode.OK

            hentetBrevutfall.brevutfall?.aldersgruppe shouldBe dto.brevutfall?.aldersgruppe
            hentetBrevutfall.brevutfall?.lavEllerIngenInntekt shouldBe dto.brevutfall?.lavEllerIngenInntekt
        }
    }

    @Test
    fun `skal hente etterbetaling`() {
        val behandlingId = UUID.randomUUID()
        val dto = brevutfallOgEtterbetalingDto(behandlingId)

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hentEtterbetaling(any()) } returns etterbetaling(behandlingId)

        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.get("/api/behandling/${UUID.randomUUID()}/info/etterbetaling") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val etterbetaling: EtterbetalingDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            etterbetaling.datoFom shouldBe dto.etterbetaling?.datoFom
            etterbetaling.datoTom shouldBe dto.etterbetaling?.datoTom
        }
    }

    private fun behandling(behandlingId: UUID): Behandling =
        mockk {
            every { id } returns behandlingId
            every { sak } returns
                mockk {
                    every { sakType } returns SakType.BARNEPENSJON
                }
            every { status } returns BehandlingStatus.BEREGNET
            every { virkningstidspunkt } returns
                Virkningstidspunkt.create(YearMonth.of(2023, 1), "ident", "begrunnelse")
        }

    private fun brevutfall(behandlingId: UUID) =
        Brevutfall(
            behandlingId = behandlingId,
            aldersgruppe = Aldersgruppe.UNDER_18,
            lavEllerIngenInntekt = LavEllerIngenInntekt.JA,
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
        )

    private fun etterbetaling(behandlingId: UUID) =
        Etterbetaling(
            behandlingId = behandlingId,
            fom = YearMonth.of(2023, 1),
            tom = YearMonth.of(2023, 2),
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
        )

    private fun brevutfallOgEtterbetalingDto(behandlingId: UUID = UUID.randomUUID()) =
        BrevutfallOgEtterbetalingDto(
            behandlingId = UUID.randomUUID(),
            brevutfall = BrevutfallDto(behandlingId, Aldersgruppe.UNDER_18, LavEllerIngenInntekt.JA, null),
            etterbetaling =
                EtterbetalingDto(
                    behandlingId = behandlingId,
                    datoFom = LocalDate.of(2023, 1, 1),
                    datoTom = LocalDate.of(2023, 2, 28),
                    null,
                ),
        )

    private fun ApplicationTestBuilder.createClient() =
        createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to "John Doe",
                    "NAVident" to "Saksbehandler01",
                ),
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "mock-client-id"
    }
}
