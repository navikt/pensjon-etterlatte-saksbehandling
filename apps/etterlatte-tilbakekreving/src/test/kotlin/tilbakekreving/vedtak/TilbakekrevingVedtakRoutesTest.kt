package tilbakekreving.vedtak

import io.kotest.matchers.shouldBe
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
import no.nav.etterlatte.ktor.issueSystembrukerToken
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.tilbakekreving.tilbakekrevingsvedtak
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingVedtakService
import no.nav.etterlatte.tilbakekreving.vedtak.tilbakekrevingVedtakRoutes
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingVedtakRoutesTest {
    private val server = MockOAuth2Server()
    private val service = mockk<TilbakekrevingVedtakService>()

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
                client.post("/api/tilbakekreving/tilbakekrevingsvedtak") {
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
                client.post("/api/tilbakekreving/tilbakekrevingsvedtak") {
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

    private fun testApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
        io.ktor.server.testing.testApplication {
            runServer(server) {
                tilbakekrevingVedtakRoutes(service)
            }
            block(this)
        }
    }

    private val token: String by lazy { server.issueSystembrukerToken() }
}
