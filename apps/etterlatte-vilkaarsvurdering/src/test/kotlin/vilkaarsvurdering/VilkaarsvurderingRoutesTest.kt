package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.restModule
import no.nav.etterlatte.testsupport.tokenTestSupportAcceptsAllTokens
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VilkaarsvurderingRoutesTest {

    private val applicationContext: ApplicationContext = mockk {
        every { tokenValidering } returns AuthenticationConfig::tokenTestSupportAcceptsAllTokens
    }

    @Test
    fun `skal hente vilkaarsvurdering`() {
        withTestApplication({ restModule(applicationContext) }) {
            with(
                handleRequest(HttpMethod.Get, "/vilkaarsvurdering") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
                }
            ) {
                assertEquals(HttpStatusCode.OK, response.status())
                // TODO verifisere respons
            }
        }
    }

    private val saksbehandlerToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibm" +
            "FtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w" +
            "8AvX5BXxlEA8U-UAOtdG1Ix_kQY"
}