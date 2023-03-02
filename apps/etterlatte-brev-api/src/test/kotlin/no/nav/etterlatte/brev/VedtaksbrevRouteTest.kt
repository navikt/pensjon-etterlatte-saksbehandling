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
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.person.Foedselsnummer
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
    fun `Endepunkt for oppretting eller oppdatering av vedtaksbrev`() {
        coEvery { vedtaksbrevService.oppdaterVedtaksbrev(any(), any(), any()) } returns opprettBrev()

        val sakId = 123456L
        val token = accessToken

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log, routePrefix = "api") { vedtaksbrevRoute(vedtaksbrevService) } }

            val client = createClient {
                install(ContentNegotiation) {
                    jackson()
                }
            }

            val response = client.post("/api/brev/vedtak") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(OpprettVedtaksbrevRequest(sakId, BEHANDLING_ID))
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) {
            vedtaksbrevService.oppdaterVedtaksbrev(
                sakId,
                BEHANDLING_ID,
                any()
            )
        }
    }

    @Test
    fun `Endepunkt som ikke finnes`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log, routePrefix = "api") { vedtaksbrevRoute(vedtaksbrevService) } }

            val response = client.get("/api/brev/finnesikke") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify { vedtaksbrevService wasNot Called }
    }

    @Test
    fun `Mangler auth header`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log, routePrefix = "api") { vedtaksbrevRoute(vedtaksbrevService) } }

            val response = client.post("/api/brev/vedtak")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        verify { vedtaksbrevService wasNot Called }
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

    private fun opprettBrev() = Brev(
        1,
        BEHANDLING_ID,
        "soeker_fnr",
        "tittel",
        Status.OPPRETTET,
        Mottaker(STOR_SNERK),
        true
    )

    companion object {
        private const val ISSUER_ID = "azure"
        private const val CLIENT_ID = "mock-client-id"
        private val STOR_SNERK = Foedselsnummer.of("11057523044")
        private val BEHANDLING_ID = UUID.randomUUID()
    }
}