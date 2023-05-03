package no.nav.etterlatte.brev

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.tilgangssjekk.BehandlingKlient
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
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
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksbrevRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val vedtaksbrevService = mockk<VedtaksbrevService>()
    private val behandlingKlient = mockk<BehandlingKlient>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.start()
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
    fun `Endepunkt for oppretting eller oppdatering av vedtaksbrev`() {
        coEvery { vedtaksbrevService.oppdaterVedtaksbrev(any(), any(), any()) } returns opprettBrev()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        val sakId = 123456L
        val token = accessToken

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") {
                    vedtaksbrevRoute(
                        vedtaksbrevService,
                        behandlingKlient
                    )
                }
            }

            val client = createClient {
                install(ContentNegotiation) {
                    jackson()
                }
            }

            val response = client.post("/api/brev/behandling/$BEHANDLING_ID/vedtak") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(OpprettVedtaksbrevRequest(sakId))
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) {
            vedtaksbrevService.oppdaterVedtaksbrev(
                sakId,
                BEHANDLING_ID,
                any()
            )
            behandlingKlient.harTilgangTilBehandling(any(), any())
        }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") {
                    vedtaksbrevRoute(
                        vedtaksbrevService,
                        behandlingKlient
                    )
                }
            }

            val response = client.get("/api/brev/finnesikke") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify { vedtaksbrevService wasNot Called }
        verify { behandlingKlient wasNot Called }
    }

    @Test
    fun `Mangler auth header`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") {
                    vedtaksbrevRoute(
                        vedtaksbrevService,
                        behandlingKlient
                    )
                }
            }

            val response = client.post("/api/brev/behandling/${UUID.randomUUID()}/vedtak")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        verify { vedtaksbrevService wasNot Called }
        verify { behandlingKlient wasNot Called }
    }

    private val accessToken: String by lazy {
        mockOAuth2Server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "Test Veiledersen",
                "NAVident" to "S123456"
            )
        ).serialize()
    }

    private fun opprettBrev() = Brev(
        1,
        BEHANDLING_ID,
        "soeker_fnr",
        "tittel",
        Status.OPPRETTET,
        Mottaker(
            "Stor Snerk",
            STOR_SNERK,
            null,
            Adresse(adresseType = "NORSKPOSTADRESSE", "Testgaten 13", "1234", "OSLO", land = "Norge", landkode = "NOR")
        ),
        true
    )

    companion object {
        private const val CLIENT_ID = "mock-client-id"
        private val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}