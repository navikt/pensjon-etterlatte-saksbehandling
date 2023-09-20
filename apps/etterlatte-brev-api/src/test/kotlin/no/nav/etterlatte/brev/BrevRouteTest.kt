package no.nav.etterlatte.brev

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import no.nav.etterlatte.brev.BrevService.BrevPayload
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
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
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevRouteTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val brevService = mockk<BrevService>()
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
        val brevId = Random.nextLong()

        coEvery { brevService.hentBrev(any()) } returns opprettBrev(brevId)
        coEvery { behandlingKlient.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/$brevId") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify { brevService.hentBrev(brevId) }
    }

    @Test
    fun `Endepunkt for generering av pdf`() {
        val brevId = Random.nextLong()
        val pdf = Pdf("Hello world".toByteArray())

        coEvery { brevService.genererPdf(any(), any()) } returns pdf
        coEvery { behandlingKlient.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/$brevId/pdf") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertArrayEquals(pdf.bytes, response.body())
        }

        coVerify(exactly = 1) {
            brevService.genererPdf(brevId, any())
            behandlingKlient.harTilgangTilSak(any(), any())
        }
    }

    @Test
    fun `Endepunkt for henting av manuelt brev`() {
        val brevId = Random.nextLong()

        coEvery { brevService.hentBrevPayload(any()) } returns BrevPayload(Slate(), null)
        coEvery { behandlingKlient.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/$brevId/payload") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) {
            brevService.hentBrevPayload(brevId)
            behandlingKlient.harTilgangTilSak(any(), any())
        }
    }

    @Test
    fun `Endepunkt for lagring av manuelt brev`() {
        val brevId = Random.nextLong()
        coEvery { brevService.lagreBrevPayload(any(), any()) } returns 1
        coEvery { brevService.lagreBrevPayloadVedlegg(any(), any()) } returns 1
        coEvery { behandlingKlient.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.post("/api/brev/$brevId/payload") {
                    parameter("sakId", SAK_ID)
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(OppdaterPayloadRequest(Slate(), listOf()))
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) {
            brevService.lagreBrevPayload(brevId, any())
            behandlingKlient.harTilgangTilSak(any(), any())
        }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        coEvery { behandlingKlient.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            val client = httpClient()

            val response =
                client.get("/api/brev/123/baretull") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify {
            brevService wasNot Called
            behandlingKlient wasNot Called
        }
    }

    @Test
    fun `Mangler auth header`() {
        testApplication {
            val client = httpClient()

            val response = client.get("/api/brev/123")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        verify {
            brevService wasNot Called
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

    private fun opprettBrev(id: BrevID) =
        Brev(
            id = id,
            sakId = 41,
            behandlingId = null,
            prosessType = BrevProsessType.AUTOMATISK,
            soekerFnr = "soeker_fnr",
            status = Status.OPPRETTET,
            mottaker =
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
                brevRoute(
                    brevService,
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
        private val SAK_ID = Random.nextLong(1000)
    }
}
