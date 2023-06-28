package sak

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.sak.sakSystemRoutes
import no.nav.etterlatte.sak.sakWebRoutes
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakRoutesTest {

    private val mockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val generellBehandlingService = mockk<GenerellBehandlingService>(relaxUnitFun = true)
    private val sakService = mockk<SakService>(relaxUnitFun = true)
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>(relaxUnitFun = true)
    private val tilgangService = mockk<TilgangService>(relaxUnitFun = true)

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
    fun `siste iverksatte route returnerer 200 ok og behandling`() {
        val sakId = 1
        coEvery { generellBehandlingService.hentSisteIverksatte(1) } returns mockk(relaxed = true)

        withTestApplication { client ->
            val response = client.get("/api/saker/$sakId/behandlinger/sisteIverksatte") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `siste iverksatte route returnerer 404 naar det ikke finnes noen iverksatt behandling`() {
        val sakId = 1
        coEvery { generellBehandlingService.hentSisteIverksatte(1) } returns null
        withTestApplication { client ->
            val response = client.get("/saker/$sakId/behandlinger/sisteIverksatte") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(404, response.status.value)
        }
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log) {
                    sakSystemRoutes(
                        tilgangService,
                        sakService,
                        generellBehandlingService
                    )
                    sakWebRoutes(
                        tilgangService,
                        sakService,
                        generellBehandlingService,
                        grunnlagsendringshendelseService
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
        val sakId: Long = 1
        const val NAVident = "Saksbehandler01"
        const val CLIENT_ID = "mock-client-id"
    }
}