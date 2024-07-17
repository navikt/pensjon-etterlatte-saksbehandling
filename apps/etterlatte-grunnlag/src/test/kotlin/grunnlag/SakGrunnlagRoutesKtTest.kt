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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakGrunnlagRoutesKtTest {
    private val grunnlagService = mockk<GrunnlagService>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val server = MockOAuth2Server()

    @BeforeAll
    fun before() {
        server.start()
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
                        append(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
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
                        append(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                    }
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(serialize(testData), response.body<String>())
        }

        verify(exactly = 1) { grunnlagService.hentOpplysningsgrunnlagForSak(sakId) }
        coVerify(exactly = 1) { behandlingKlient.harTilgangTilSak(sakId, any(), any()) }
    }

    private fun ApplicationTestBuilder.createHttpClient(): HttpClient =
        runServer(server, "api/grunnlag") { sakGrunnlagRoute(grunnlagService, behandlingKlient) }
}
