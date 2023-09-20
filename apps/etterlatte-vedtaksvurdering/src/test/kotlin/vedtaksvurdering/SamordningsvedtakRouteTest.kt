package vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.vedtaksvurdering.samordningsvedtakRoute
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SamordningsvedtakRouteTest {
    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val vedtaksvurderingService: VedtaksvurderingService = mockk()

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal returnere 401 naar token mangler noedvendig rolle`() {
        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { samordningsvedtakRoute(vedtaksvurderingService) } }

            val response =
                client.get("/api/samordning/vedtak/1234") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${token(listOf("dummy"))}")
                }

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `skal returnere vedtak naar token har noedvendig rolle og vedtak eksisterer`() {
        coEvery { vedtaksvurderingService.hentVedtak(1234) } returns vedtak()

        testApplication {
            environment { config = applicationConfig }
            application { restModule(log) { samordningsvedtakRoute(vedtaksvurderingService) } }

            val response =
                client.get("/api/samordning/vedtak/1234") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${token(listOf("dummy", "samordning-read"))}")
                }

            response.status shouldBe HttpStatusCode.OK
            coVerify { vedtaksvurderingService.hentVedtak(1234) }
        }
    }

    private fun token(roles: List<String>): String =
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("roles" to roles)
        ).serialize()

    private companion object {
        const val CLIENT_ID = "azure-id for app"
    }
}