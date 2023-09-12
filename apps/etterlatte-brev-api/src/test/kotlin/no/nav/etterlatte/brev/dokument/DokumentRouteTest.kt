package no.nav.etterlatte.brev.dokument

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.journalpost.BrukerIdType
import no.nav.etterlatte.brev.journalpost.FerdigstillJournalpostRequest
import no.nav.etterlatte.libs.common.FoedselsnummerDTO
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
    private val behandlingKlient = mockk<BehandlingKlient>()
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
    fun `Endepunkt for uthenting av alle dokumenter tilknyttet brukeren`() {
        coEvery { journalpostService.hentDokumenter(any(), any(), any()) } returns HentJournalposterResult()
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
                        dokarkivService,
                        behandlingKlient
                    )
                }
            }
            val httpClient = createClient {
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
            }
            val response = httpClient.post("/api/dokumenter") {
                contentType(ContentType.Application.Json)
                setBody(FoedselsnummerDTO(fnr))
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) { journalpostService.hentDokumenter(fnr, BrukerIdType.FNR, any()) }
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
    fun `Endepunkt for å ferdigstille journalpost`() {
        coEvery { dokarkivService.ferdigstill(any(), any()) } just Runs

        val journalpostId = "111"

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log, routePrefix = "api") {
                    dokumentRoute(
                        journalpostService,
                        dokarkivService,
                        behandlingKlient
                    )
                }
            }

            val client = createClient {
                install(ContentNegotiation) {
                    jackson()
                }
            }

            val response = client.post("/api/dokumenter/$journalpostId/ferdigstill") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(FerdigstillJournalpostRequest("4808"))
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) { dokarkivService.ferdigstill(journalpostId, any()) }
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
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "Test Veiledersen",
                "NAVident" to "S123456"
            )
        ).serialize()
    }

    companion object {
        private const val CLIENT_ID = "mock-client-id"
    }
}