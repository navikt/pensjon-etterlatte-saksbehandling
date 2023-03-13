package no.nav.etterlatte.dokument

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.dokument.JournalpostResponse
import no.nav.etterlatte.brev.dokument.SafClient
import no.nav.etterlatte.brev.dokument.dokumentRoute
import no.nav.etterlatte.brev.tilgangssjekk.BehandlingKlient
import no.nav.etterlatte.libs.common.journalpost.BrukerIdType
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DokumentRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val journalpostService = mockk<SafClient>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.start()
        val httpServer = mockOAuth2Server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), ISSUER_ID, CLIENT_ID)
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
    fun `Endepunkt for uthenting av alle dokumenter tilknyttet brukeren`() {
        coEvery { journalpostService.hentDokumenter(any(), any(), any()) } returns JournalpostResponse(null, null)
        coEvery { behandlingKlient.harTilgangTilPerson(any(), any()) } returns true

        val token = accessToken
        val fnr = "26117512737"

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") {
                    dokumentRoute(
                        journalpostService,
                        behandlingKlient
                    )
                }
            }

            val response = client.get("/api/dokumenter/$fnr") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) { journalpostService.hentDokumenter(fnr, BrukerIdType.FNR, token) }
    }

    @Test
    fun `Endepunkt for uthenting av bestemt dokument`() {
        coEvery { journalpostService.hentDokumentPDF(any(), any(), any()) } returns "dokument".toByteArray()

        val journalpostId = "111"
        val dokumentInfoId = "333"

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") {
                    dokumentRoute(
                        journalpostService,
                        behandlingKlient
                    )
                }
            }

            val response = client.get("/api/dokumenter/$journalpostId/$dokumentInfoId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) { journalpostService.hentDokumentPDF(journalpostId, dokumentInfoId, any()) }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") {
                    dokumentRoute(
                        journalpostService,
                        behandlingKlient
                    )
                }
            }

            val response = client.get("/api/dokument/finnesikke") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify { journalpostService wasNot Called }
    }

    private val accessToken: String by lazy {
        mockOAuth2Server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "Test Veiledersen",
                "NAVident" to "S123456"
            )
        ).serialize()
    }

    companion object {
        private const val ISSUER_ID = "azure"
        private const val CLIENT_ID = "mock-client-id"
    }
}