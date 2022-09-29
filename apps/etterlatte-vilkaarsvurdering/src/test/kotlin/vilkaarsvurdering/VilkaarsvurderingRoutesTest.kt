package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.client.request.delete
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
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.restModule
import no.nav.etterlatte.testsupport.tokenTestSupportAcceptsAllTokens
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VilkaarsvurderingRoutesTest {

    private val vilkaarsvurderingServiceImpl = VilkaarsvurderingService(VilkaarsvurderingRepositoryInMemory())

    private val applicationContext: ApplicationContext = mockk {
        every { tokenValidering } returns AuthenticationConfig::tokenTestSupportAcceptsAllTokens
        every { vilkaarsvurderingService } returns vilkaarsvurderingServiceImpl
    }

    @Test
    fun `skal hente vilkaarsvurdering`() {
        testApplication {
            application { restModule(applicationContext) }

            val response = client.get("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), Vilkaarsvurdering::class.java)
            val vilkaar = vilkaarsvurdering.vilkaar.first()

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(behandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(VilkaarType.FORMAAL, vilkaar.type)
            assertEquals("§ 18-1", vilkaar.paragraf.paragraf)
            assertEquals("Formål", vilkaar.paragraf.tittel)
            assertEquals(
                "Formålet med barnepensjon er å sikre inntekt for barn når en av foreldrene eller begge er døde.",
                vilkaar.paragraf.lovtekst
            )
            assertEquals("https://lovdata.no/lov/1997-02-28-19/%C2%A718-1", vilkaar.paragraf.lenke)
            assertNull(vilkaar.vurdering)
        }
    }

    @Test
    fun `skal oppdatere en vilkaarsvurdering med et vurdert vilkaar`() {
        testApplication {
            application { restModule(applicationContext) }

            // Oppretter vilkaarsvurdering
            client.get("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            val vurdertResultatDto = VurdertResultatDto(
                type = VilkaarType.FORMAAL,
                resultat = Utfall.OPPFYLT,
                kommentar = "Søker oppfyller vilkåret"
            )

            val oppdatertVilkaarsvurderingResponse = client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertResultatDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            val oppdatertVilkaarsvurdering = objectMapper
                .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), Vilkaarsvurdering::class.java)
            val oppdatertVilkaar = oppdatertVilkaarsvurdering.vilkaar.find { it.type == vurdertResultatDto.type }

            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(vurdertResultatDto.type, oppdatertVilkaar?.type)
            assertEquals(vurdertResultatDto.resultat, oppdatertVilkaar?.vurdering?.resultat)
            assertEquals(vurdertResultatDto.kommentar, oppdatertVilkaar?.vurdering?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaar?.vurdering?.saksbehandler)
            assertNotNull(oppdatertVilkaar?.vurdering?.tidspunkt)
        }
    }

    @Test
    fun `skal nullstille et vurdert vilkaar fra vilkaarsvurdering`() {
        testApplication {
            application { restModule(applicationContext) }

            // Oppretter vilkaarsvurdering
            client.get("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            val vurdertResultatDto = VurdertResultatDto(
                type = VilkaarType.FORMAAL,
                resultat = Utfall.OPPFYLT,
                kommentar = "Søker oppfyller vilkåret"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertResultatDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            val vurdertResultat = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId).vilkaar
                .first { it.type == vurdertResultatDto.type }.vurdering

            assertNotNull(vurdertResultat)

            val response = client.delete("/api/vilkaarsvurdering/$behandlingId/${vurdertResultatDto.type}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $saksbehandlerToken")
            }

            val vurdertResultatSlettet = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId).vilkaar
                .first { it.type == vurdertResultatDto.type }.vurdering

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(vurdertResultatSlettet)
        }
    }

    private companion object {
        val behandlingId = "1"
        val saksbehandlerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIG" +
            "ZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZW" +
            "hhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY"
    }
}