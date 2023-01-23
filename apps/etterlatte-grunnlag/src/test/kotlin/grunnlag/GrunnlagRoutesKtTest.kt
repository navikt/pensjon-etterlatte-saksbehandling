package no.nav.etterlatte.grunnlag

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagRoutesKtTest {
    private val grunnlagService = mockk<GrunnlagService>()
    private val server = MockOAuth2Server()

    companion object {
        private const val ISSUER_ID = "ISSUER_ID"
        private const val CLIENT_ID = "CLIENT_ID"
    }

    @BeforeAll
    fun before() {
        server.start()
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", server.wellKnownUrl(ISSUER_ID).toString())
        System.setProperty("AZURE_APP_CLIENT_ID", CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    private val token by lazy {
        server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "Per Persson",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    @Test
    fun `returnerer 401 uten gyldig token`() {
        testApplication {
            application { restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService) } }
            val response = client.get("api/grunnlag/1")

            Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `returnerer 404 hvis grunnlag ikke finnes`() {
        every { grunnlagService.hentOpplysningsgrunnlag(any()) } returns null

        testApplication {
            application { restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService) } }
            val response = client.get("api/grunnlag/1") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            Assertions.assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `200 ok gir mapped data`() {
        val testData = GrunnlagTestData().hentOpplysningsgrunnlag()
        every { grunnlagService.hentOpplysningsgrunnlag(1) } returns testData

        testApplication {
            application { restModule(this.log, routePrefix = "api") { grunnlagRoute(grunnlagService) } }
            val response = client.get("api/grunnlag/1") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            Assertions.assertEquals(HttpStatusCode.OK, response.status)
            Assertions.assertEquals(serialize(testData), response.body<String>())
        }
    }
}