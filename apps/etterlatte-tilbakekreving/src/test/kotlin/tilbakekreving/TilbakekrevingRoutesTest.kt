package no.nav.etterlatte.tilbakekreving

import io.ktor.auth.Authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.restModule
import no.nav.etterlatte.testsupport.kravgrunnlagId
import no.nav.etterlatte.testsupport.mottattKravgrunnlag
import no.nav.etterlatte.testsupport.tokenTestSupportAcceptsAllTokens
import no.nav.etterlatte.tilbakekreving.config.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TilbakekrevingRoutesTest {

    private val applicationContext: ApplicationContext = mockk {
        every { tokenValidering } returns Authentication.Configuration::tokenTestSupportAcceptsAllTokens
        every { tilbakekrevingService } returns mockk {
            every { hentTilbakekreving(1) } returns mottattKravgrunnlag()
            every { hentTilbakekreving(2) } returns null
        }
    }

    @Test
    fun `skal hente kravgrunnlag`() {
        withTestApplication({ restModule(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/tilbakekreving/${kravgrunnlagId.value}") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                // TODO verifisere respons
            }
        }
    }

    @Test
    fun `skal returnere 404 hvis kravgrunnlag mangler`() {
        withTestApplication({ restModule(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/tilbakekreving/2") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }

    @Test
    fun `skal feile med 500 dersom id ikke er gyldig`() {
        withTestApplication({ restModule(applicationContext) }) {
            handleRequest(HttpMethod.Get, "/tilbakekreving/ugyldig") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                assertEquals("En feil oppstod: For input string: \"ugyldig\"", response.content)
            }
        }
    }

    private val saksbehandlerToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibm" +
                "FtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w" +
                "8AvX5BXxlEA8U-UAOtdG1Ix_kQY"
}


