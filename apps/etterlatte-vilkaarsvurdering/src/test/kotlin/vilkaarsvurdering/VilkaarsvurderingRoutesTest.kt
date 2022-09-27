package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.restModule
import no.nav.etterlatte.testsupport.tokenTestSupportAcceptsAllTokens
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VilkaarsvurderingRoutesTest {

    private val applicationContext: ApplicationContext = mockk {
        every { tokenValidering } returns AuthenticationConfig::tokenTestSupportAcceptsAllTokens
        every { vilkaarsvurderingService } returns VilkaarsvurderingService(VilkaarsvurderingRepositoryInMemory())
    }

    @Test
    fun `skal hente vilkaarsvurdering og oppdatere`() {
        testApplication {
            application { restModule(applicationContext) }

            val behandlingId = "1"
            val vilkaarsvurderingResponse = client.get("/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            assertEquals(HttpStatusCode.OK, vilkaarsvurderingResponse.status)
            println(vilkaarsvurderingResponse.bodyAsText())

            val vurdertResultatDto = VurdertResultatDto(
                type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP,
                resultat = Utfall.OPPFYLT,
                kommentar = "Bodd i Norge hele livet"
            )

            val responseUpdate = client.post("/vilkaarsvurdering/1") {
                setBody(vurdertResultatDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            assertEquals(HttpStatusCode.OK, responseUpdate.status)
            println(responseUpdate.bodyAsText())
            // assertEquals(Vilkaar("§18.1 Blabla"), objectMapper.readValue(response.bodyAsText(), Vilkaar::class.java))
        }
    }

    private val saksbehandlerToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibm" +
            "FtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w" +
            "8AvX5BXxlEA8U-UAOtdG1Ix_kQY"
}