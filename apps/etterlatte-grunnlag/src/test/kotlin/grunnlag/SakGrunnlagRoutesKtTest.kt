package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakGrunnlagRoutesKtTest {
    private val grunnlagService = mockk<GrunnlagService>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    companion object {
        private const val CLIENT_ID = "CLIENT_ID"
    }

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(grunnlagService, behandlingKlient)
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    private val token by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to "Per Persson",
                    "NAVident" to "Saksbehandler01",
                ),
        ).serialize()
    }

    @Test
    fun `returnerer 401 uten gyldig token`() {
        val sakId = Random.nextLong()

        testApplication {
            val response = createHttpClient().get("api/grunnlag/sak/$sakId")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        coVerify(exactly = 1) { behandlingKlient wasNot Called }
    }

    @Test
    fun `returnerer 404 hvis grunnlag ikke finnes`() {
        val sakId = Random.nextLong()

        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns null
        coEvery { behandlingKlient.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get("api/grunnlag/sak/$sakId") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlagForSak(any()) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilSak(any(), any(), any()) }
    }

    @Test
    fun `Hent grunnlag for sak`() {
        val sakId = Random.nextLong()
        val testData = GrunnlagTestData().hentOpplysningsgrunnlag()

        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns testData
        coEvery { behandlingKlient.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get("api/grunnlag/sak/$sakId") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(testData), response.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlagForSak(sakId) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilSak(sakId, any(), any()) }
    }

    @Test
    fun `Hent alle personer i sak`() {
        val sakId = Random.nextLong()
        val testData =
            mapOf(
                SOEKER_FOEDSELSNUMMER to PersonMedNavn(SOEKER_FOEDSELSNUMMER, "John", "Doe", null),
            )

        every { grunnlagService.hentPersonerISak(any()) } returns testData
        coEvery { behandlingKlient.harTilgangTilSak(any(), any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get("api/grunnlag/sak/$sakId/personer/alle") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(PersonerISakDto(testData)), response.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentPersonerISak(sakId) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilSak(sakId, any(), any()) }
    }

    private fun ApplicationTestBuilder.createHttpClient(): HttpClient {
        environment {
            config = hoconApplicationConfig
        }
        application {
            restModule(this.log, routePrefix = "api/grunnlag") {
                sakGrunnlagRoute(grunnlagService, behandlingKlient)
            }
        }

        return createClient {
            install(ContentNegotiation) {
                jackson { registerModule(JavaTimeModule()) }
            }
        }
    }
}
