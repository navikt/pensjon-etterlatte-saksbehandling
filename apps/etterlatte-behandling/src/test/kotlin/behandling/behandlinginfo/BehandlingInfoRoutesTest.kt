package no.nav.etterlatte.behandling.behandlinginfo

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.config.ApplicationContext
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.EtterbetalingNy
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
        every { applicationContext.behandlingInfoService } returns BehandlingInfoService(behandlingInfoDao, behandlingService)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal lagre brevutfall`() {
        val behandlingId = UUID.randomUUID()
        val opprettDto = brevutfallDto()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.lagre(any()) } returns brevutfall(behandlingId)

        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.post("/api/behandling/${UUID.randomUUID()}/info/brevutfall") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(brevutfallDto())
                }

            val opprettetBrevutfall: BrevutfallDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            opprettetBrevutfall.aldersgruppe shouldBe opprettDto.aldersgruppe
            opprettetBrevutfall.etterbetaling shouldBe opprettDto.etterbetaling
            opprettetBrevutfall.kilde shouldNotBe null
        }
    }

    @Test
    fun `skal hente brevutfall`() {
        val behandlingId = UUID.randomUUID()
        val opprettDto = brevutfallDto()

        every { behandlingService.hentBehandling(any()) } returns behandling(behandlingId)
        every { behandlingInfoDao.hent(any()) } returns brevutfall(behandlingId)

        testApplication {
            environment { config = hoconApplicationConfig }
            application { module(applicationContext) }

            val client = createClient()

            val response =
                client.get("/api/behandling/${UUID.randomUUID()}/info/brevutfall") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentetBrevutfall: BrevutfallDto = response.body()
            response.status shouldBe HttpStatusCode.OK

            hentetBrevutfall.aldersgruppe shouldBe opprettDto.aldersgruppe
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
            etterbetalingNy = EtterbetalingNy(YearMonth.of(2023, 1), YearMonth.of(2023, 2)),
            aldersgruppe = Aldersgruppe.UNDER_18,
            kilde = Grunnlagsopplysning.Saksbehandler.create("Saksbehandler01"),
        )

    private fun brevutfallDto() =
        BrevutfallDto(
            behandlingId = UUID.randomUUID(),
            etterbetaling =
                EtterbetalingDto(
                    datoFom = LocalDate.of(2023, 1, 1),
                    datoTom = LocalDate.of(2023, 2, 28),
                ),
            aldersgruppe = Aldersgruppe.UNDER_18,
            kilde = null,
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
