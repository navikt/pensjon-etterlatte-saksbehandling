package no.nav.etterlatte.brev.dokument

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
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DokumentRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val tilgangssjekker = mockk<Tilgangssjekker>()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val journalpostService = mockk<SafClient>()
    private val dokarkivService = mockk<DokarkivService>()

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
                        dokarkivService,
                        tilgangssjekker,
                    )
                }
            }

            val response =
                client.get("/api/dokumenter/$journalpostId/$dokumentInfoId") {
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
                        dokarkivService,
                        tilgangssjekker,
                    )
                }
            }

            val response =
                client.get("/api/dokument/finnesikke") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify { journalpostService wasNot Called }
    }

    private val accessToken: String by lazy {
        mockOAuth2Server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to "Test Veiledersen",
                    "NAVident" to "S123456",
                ),
        ).serialize()
    }

    companion object {
        private const val CLIENT_ID = "mock-client-id"
    }
}
