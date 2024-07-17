package no.nav.etterlatte.tilbakekreving

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.toJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingRoutesTest {
    private val server = MockOAuth2Server()
    private val service = mockk<TilbakekrevingService>()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
        confirmVerified(service)
    }

    @Test
    fun `skal returnere 201 når tilbakekrevingsvedtak opprettes`() {
        val vedtak = tilbakekrevingsvedtak(1)
        coEvery { service.sendTilbakekrevingsvedtak(vedtak) } just runs

        testApplication {
            val response =
                client.post("/api/tilbakekreving/${vedtak.sakId}/vedtak") {
                    setBody(vedtak.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.Created
        }

        coVerify(exactly = 1) {
            service.sendTilbakekrevingsvedtak(vedtak)
        }
    }

    @Test
    fun `skal returnere 500 hvis mottak av tilbakekrevingsvedtak feiler`() {
        val vedtak = tilbakekrevingsvedtak(2)
        coEvery { service.sendTilbakekrevingsvedtak(vedtak) } throws Exception("Noe feilet")

        testApplication {
            val response =
                client.post("/api/tilbakekreving/${vedtak.sakId}/vedtak") {
                    setBody(vedtak.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.InternalServerError
        }

        coVerify(exactly = 1) {
            service.sendTilbakekrevingsvedtak(vedtak)
        }
    }

    @Test
    fun `skal returnere 200 og oppdatert kravgrunnlag ved henting av kravgrunnlag`() {
        val oppdatertKravgrunnlag = kravgrunnlag()
        val sakId = oppdatertKravgrunnlag.sakId.value
        val kravgrunnlagId = oppdatertKravgrunnlag.kravgrunnlagId.value
        coEvery { service.hentKravgrunnlag(kravgrunnlagId, sakId) } returns
            oppdatertKravgrunnlag

        testApplication {
            val response =
                client.get("/api/tilbakekreving/$sakId/kravgrunnlag/$kravgrunnlagId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
        }

        coVerify(exactly = 1) {
            service.hentKravgrunnlag(kravgrunnlagId, sakId)
        }
    }

    @Test
    fun `skal returnere 500 hvis henting av kravgrunnlag feiler`() {
        val sakId = 2L
        val kravgrunnlagId = 2L
        coEvery { service.hentKravgrunnlag(kravgrunnlagId, sakId) } throws Exception("Noe feilet")

        testApplication {
            val response =
                client.get("/api/tilbakekreving/$sakId/kravgrunnlag/$kravgrunnlagId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.InternalServerError
        }

        coVerify(exactly = 1) {
            service.hentKravgrunnlag(kravgrunnlagId, sakId)
        }
    }

    private fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        io.ktor.server.testing.testApplication {
            runServer(server) {
                tilbakekrevingRoutes(service)
            }
            block(this)
        }
    }

    private val token: String by lazy { server.issueSystembrukerToken() }
}
