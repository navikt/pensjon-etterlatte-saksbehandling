package no.nav.etterlatte.brev

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.UUID
import kotlin.random.Random

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
    fun `Endepunkt for henting av vedtaksbrev - brev finnes`() {
        coEvery { vedtaksbrevService.hentVedtaksbrev(any()) } returns opprettBrev()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/behandling/$BEHANDLING_ID/vedtak") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify { vedtaksbrevService.hentVedtaksbrev(BEHANDLING_ID) }
    }

    @Test
    fun `Endepunkt for henting av vedtaksbrev - brev finnes ikke`() {
        coEvery { vedtaksbrevService.hentVedtaksbrev(any()) } returns null
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/behandling/$BEHANDLING_ID/vedtak") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.NoContent, response.status)
        }

        coVerify {
            vedtaksbrevService.hentVedtaksbrev(BEHANDLING_ID)
            behandlingKlient.harTilgangTilBehandling(any(), any())
        }
    }

    @Test
    fun `Endepunkt for oppretting av vedtaksbrev`() {
        coEvery { vedtaksbrevService.opprettVedtaksbrev(any(), any(), any()) } returns opprettBrev()
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.post("/api/brev/behandling/$BEHANDLING_ID/vedtak") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.Created, response.status)
        }

        coVerify(exactly = 1) {
            vedtaksbrevService.opprettVedtaksbrev(SAK_ID, BEHANDLING_ID, any())
            behandlingKlient.harTilgangTilBehandling(any(), any())
        }
    }

    @Test
    fun `Endepunkt for generering av pdf`() {
        val brevId = Random.nextLong()
        val pdf = Pdf("Hello world".toByteArray())
        coEvery { vedtaksbrevService.genererPdf(any(), any()) } returns pdf
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/behandling/$BEHANDLING_ID/vedtak/pdf") {
                    parameter("brevId", brevId)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertArrayEquals(pdf.bytes, response.body())
        }

        coVerify(exactly = 1) {
            vedtaksbrevService.genererPdf(brevId, any())
            behandlingKlient.harTilgangTilBehandling(any(), any())
        }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/finnesikke") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify {
            vedtaksbrevService wasNot Called
            behandlingKlient wasNot Called
        }
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
                        behandlingKlient,
                    )
                }
            }

            val response = client.post("/api/brev/behandling/${UUID.randomUUID()}/vedtak")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        verify {
            vedtaksbrevService wasNot Called
            behandlingKlient wasNot Called
        }
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

    private fun opprettBrev() =
        Brev(
            1,
            41,
            BEHANDLING_ID,
            BrevProsessType.AUTOMATISK,
            "soeker_fnr",
            Status.OPPRETTET,
            Mottaker(
                "Stor Snerk",
                STOR_SNERK,
                null,
                Adresse(adresseType = "NORSKPOSTADRESSE", "Testgaten 13", "1234", "OSLO", land = "Norge", landkode = "NOR"),
            ),
        )

    private fun ApplicationTestBuilder.httpClient(): HttpClient {
        environment {
            config = hoconApplicationConfig
        }
        application {
            restModule(this.log, routePrefix = "api") {
                vedtaksbrevRoute(
                    vedtaksbrevService,
                    behandlingKlient,
                )
            }
        }

        return createClient {
            install(ContentNegotiation) {
                jackson()
            }
        }
    }

    companion object {
        private const val CLIENT_ID = "mock-client-id"
        private val STOR_SNERK = Foedselsnummer("11057523044")
        private val BEHANDLING_ID = UUID.randomUUID()
        private val SAK_ID = Random.nextLong(1000)
    }
}
