package sak

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.config.HoconApplicationConfig
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.sak.UtlandstilknytningRequest
import no.nav.etterlatte.sak.sakSystemRoutes
import no.nav.etterlatte.sak.sakWebRoutes
import no.nav.etterlatte.withTestApplicationBuilder
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val behandlingService = mockk<BehandlingService>(relaxUnitFun = true)
    private val sakService = mockk<SakService>(relaxUnitFun = true)
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>(relaxUnitFun = true)
    private val tilgangService = mockk<TilgangService>(relaxUnitFun = true)
    private val oppgaveService = mockk<OppgaveService>(relaxUnitFun = true)
    private val requestLogger = mockk<BehandlingRequestLogger>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.start(1234)
        val httpServer = mockOAuth2Server.config.httpServer
        hoconApplicationConfig =
            buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
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
    fun `kan oppdatere utenlandstilknytning`() {
        coEvery {
            sakService.oppdaterUtenlandstilknytning(any(), any())
        } just runs

        withTestApplication { client ->
            val response =
                client.post("/api/sak/1/utenlandstilknytning") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(UtlandstilknytningRequest(UtlandstilknytningType.BOSATT_UTLAND, "Test"))
                }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `siste iverksatte route returnerer 200 ok og behandling`() {
        val sakId = 1
        coEvery { behandlingService.hentSisteIverksatte(1) } returns mockk(relaxed = true)

        withTestApplication { client ->
            val response =
                client.get("/saker/$sakId/behandlinger/sisteIverksatte") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `siste iverksatte route returnerer 404 naar det ikke finnes noen iverksatt behandling`() {
        val sakId = 1
        coEvery { behandlingService.hentSisteIverksatte(1) } returns null
        withTestApplication { client ->
            val response =
                client.get("/saker/$sakId/behandlinger/sisteIverksatte") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(404, response.status.value)
        }
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        withTestApplicationBuilder(block, hoconApplicationConfig) {
            attachMockContext()
            sakSystemRoutes(
                tilgangService,
                sakService,
                behandlingService,
                requestLogger,
            )
            sakWebRoutes(
                tilgangService,
                sakService,
                behandlingService,
                grunnlagsendringshendelseService,
                oppgaveService,
                requestLogger,
            )
        }
    }

    private val token: String by lazy {
        mockOAuth2Server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to "John Doe",
                    "NAVident" to NAV_IDENT,
                ),
        ).serialize()
    }

    private companion object {
        const val NAV_IDENT = "Saksbehandler01"
        const val CLIENT_ID = "mock-client-id"
    }
}
