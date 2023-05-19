package behandling

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.UtenlandstilsnittRequest
import no.nav.etterlatte.behandling.behandlingRoutes
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerService
import no.nav.etterlatte.libs.common.behandling.UtenlandstilsnittType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.YearMonth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingRoutesTest {

    private val mockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val generellBehandlingService = mockk<GenerellBehandlingService>(relaxUnitFun = true)
    private val foerstegangsbehandlingService = mockk<FoerstegangsbehandlingService>()
    private val manueltOpphoerService = mockk<ManueltOpphoerService>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.start(1234)
        val httpServer = mockOAuth2Server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
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
    fun `kan lagre virkningstidspunkt hvis det er gyldig`() {
        coEvery {
            generellBehandlingService.oppdaterUtenlandstilsnitt(any(), any())
        } just runs

        withTestApplication { client ->
            val response = client.post("/api/behandling/$behandlingId/utenlandstilsnitt") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(UtenlandstilsnittRequest(UtenlandstilsnittType.UTLAND_BOSATT_UTLAND, "Test"))
            }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `kan oppdater utenlandstilsnitt`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"

        mockBehandlingService(bodyVirkningstidspunkt, bodyBegrunnelse)

        coEvery {
            generellBehandlingService.erGyldigVirkningstidspunkt(any(), any(), any())
        } returns true

        withTestApplication { client ->
            val response = client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                    "dato":"$bodyVirkningstidspunkt",
                    "begrunnelse":"$bodyBegrunnelse"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `Avbryt behandling`() {
        withTestApplication { client ->
            val response = client.post("/api/behandling/$behandlingId/avbryt") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(200, response.status.value)
            verify(exactly = 1) { generellBehandlingService.avbrytBehandling(behandlingId, any()) }
        }
    }

    @Test
    fun `faar bad request hvis virkningstidspunkt ikke er gyldig`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"

        mockBehandlingService(bodyVirkningstidspunkt, bodyBegrunnelse)

        coEvery {
            generellBehandlingService.erGyldigVirkningstidspunkt(any(), any(), any())
        } returns false

        withTestApplication { client ->
            val response = client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                    "dato":"$bodyVirkningstidspunkt",
                    "begrunnelse":"$bodyBegrunnelse"
                    }
                    """.trimIndent()
                )
            }

            assertEquals(400, response.status.value)
            assertEquals("Ugyldig virkningstidspunkt", response.bodyAsText())
        }
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log) {
                    behandlingRoutes(
                        generellBehandlingService,
                        foerstegangsbehandlingService,
                        manueltOpphoerService
                    )
                }
            }

            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            block(client)
        }
    }

    private fun mockBehandlingService(bodyVirkningstidspunkt: Tidspunkt, bodyBegrunnelse: String) {
        val parsetVirkningstidspunkt = YearMonth.from(
            bodyVirkningstidspunkt.toNorskTid().let {
                YearMonth.of(it.year, it.month)
            }
        )
        val virkningstidspunkt = Virkningstidspunkt(
            parsetVirkningstidspunkt,
            Grunnlagsopplysning.Saksbehandler.create(NAVident),
            bodyBegrunnelse
        )
        every {
            generellBehandlingService.oppdaterVirkningstidspunkt(
                behandlingId,
                parsetVirkningstidspunkt,
                NAVident,
                bodyBegrunnelse

            )
        } returns virkningstidspunkt
    }

    private val token: String by lazy {
        mockOAuth2Server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to NAVident
            )
        ).serialize()
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        const val NAVident = "Saksbehandler01"
        const val CLIENT_ID = "mock-client-id"
    }
}