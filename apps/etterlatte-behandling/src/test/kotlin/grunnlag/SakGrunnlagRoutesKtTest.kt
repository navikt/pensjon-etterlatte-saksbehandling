package no.nav.etterlatte.grunnlag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.sak.TilgangServiceSjekker
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakGrunnlagRoutesKtTest {
    private val grunnlagService = mockk<GrunnlagServiceImpl>()
    private val tilgangsservice = mockk<TilgangServiceSjekker>()
    private val mockOAuth2Server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(grunnlagService, tilgangsservice)
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `returnerer 401 uten gyldig token`() {
        val sakId = randomSakId()

        testApplication {
            val response = createHttpClient().get("api/grunnlag/sak/${sakId.sakId}")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        coVerify(exactly = 1) { tilgangsservice wasNot Called }
    }

    @Test
    fun `returnerer 404 hvis grunnlag ikke finnes`() {
        val sakId = randomSakId()

        coEvery { grunnlagService.hentOpplysningsgrunnlagForSak(any(), any()) } returns null
        coEvery { tilgangsservice.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get("api/grunnlag/sak/${sakId.sakId}") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    }
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        coVerify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlagForSak(any(), any()) }
    }

    @Test
    fun `Hent grunnlag for sak`() {
        val sakId = randomSakId()
        val testData = GrunnlagTestData().hentOpplysningsgrunnlag()

        coEvery { grunnlagService.hentOpplysningsgrunnlagForSak(any(), any()) } returns testData
        coEvery { tilgangsservice.harTilgangTilSak(any(), any()) } returns true

        testApplication {
            val response =
                createHttpClient().get("api/grunnlag/sak/${sakId.sakId}") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(testData), response.body<String>())
        }

        coVerify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlagForSak(sakId, any()) }
    }

    private fun ApplicationTestBuilder.createHttpClient(): HttpClient =
        runServer(mockOAuth2Server, "api/grunnlag") { sakGrunnlagRoute(grunnlagService) }
}
