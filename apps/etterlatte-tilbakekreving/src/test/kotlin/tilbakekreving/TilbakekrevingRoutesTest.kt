package no.nav.etterlatte.tilbakekreving

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.testsupport.kravgrunnlagId
import no.nav.etterlatte.testsupport.mottattKravgrunnlag
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingRoutesTest {

    private val applicationContext: ApplicationContext = mockk {
        every { tilbakekrevingService } returns mockk {
            every { hentTilbakekreving(1) } returns mottattKravgrunnlag()
            every { hentTilbakekreving(2) } returns null
        }
    }

    private val server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal hente kravgrunnlag`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { tilbakekreving(applicationContext.tilbakekrevingService) } }

            val response = client.get("/api/tilbakekreving/${kravgrunnlagId.value}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `skal returnere 404 hvis kravgrunnlag mangler`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { tilbakekreving(applicationContext.tilbakekrevingService) } }

            val response = client.get("/api/tilbakekreving/2") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `skal feile med 500 dersom id ikke er gyldig`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { tilbakekreving(applicationContext.tilbakekrevingService) } }

            val response = client.get("/api/tilbakekreving/ugyldig") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "mock-client-id"
    }
}