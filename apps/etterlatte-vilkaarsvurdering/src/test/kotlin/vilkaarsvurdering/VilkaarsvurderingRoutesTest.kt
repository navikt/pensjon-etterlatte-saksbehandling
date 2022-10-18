package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.restModule
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationContext
import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationProperties
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.FileNotFoundException
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingRoutesTest {

    private val vilkaarsvurderingServiceImpl = VilkaarsvurderingService(VilkaarsvurderingRepositoryInMemory())
    private val server = MockOAuth2Server()

    private val applicationContext: ApplicationContext = mockk {
        every { properties } returns ApplicationProperties()
        every { vilkaarsvurderingService } returns vilkaarsvurderingServiceImpl
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

    private val token: String by lazy {
        server.issueToken(
            issuerId = ISSUER_ID,
            audience = CLIENT_ID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    @Test
    fun `skal hente vilkaarsvurdering`() {
        testApplication {
            application { restModule(applicationContext) }

            opprettVilkaarsvurdering()

            val response = client.get("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
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

            opprettVilkaarsvurdering()

            val vurdertResultatDto = VurdertResultatDto(
                type = VilkaarType.FORMAAL,
                resultat = Utfall.OPPFYLT,
                kommentar = "Søker oppfyller vilkåret"
            )

            val oppdatertVilkaarsvurderingResponse = client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertResultatDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
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

            opprettVilkaarsvurdering()

            val vurdertResultatDto = VurdertResultatDto(
                type = VilkaarType.FORMAAL,
                resultat = Utfall.OPPFYLT,
                kommentar = "Søker oppfyller vilkåret"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertResultatDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertResultat = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)?.vilkaar
                ?.first { it.type == vurdertResultatDto.type }?.vurdering

            assertNotNull(vurdertResultat)

            val response = client.delete("/api/vilkaarsvurdering/$behandlingId/${vurdertResultatDto.type}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertResultatSlettet = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)?.vilkaar
                ?.first { it.type == vurdertResultatDto.type }?.vurdering

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(vurdertResultatSlettet)
        }
    }

    @Test
    fun `skal sette og nullstille totalresultat for en vilkaarsvurdering`() {
        testApplication {
            application { restModule(applicationContext) }

            opprettVilkaarsvurdering()
            val resultat = VurdertVilkaarsvurderingResultatDto(
                resultat = VilkaarsvurderingUtfall.OPPFYLT,
                kommentar = "Søker oppfyller vurderingen"
            )
            val oppdatertVilkaarsvurderingResponse = client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                setBody(resultat.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val oppdatertVilkaarsvurdering = objectMapper
                .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), Vilkaarsvurdering::class.java)

            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(resultat.resultat, oppdatertVilkaarsvurdering?.resultat?.utfall)
            assertEquals(resultat.kommentar, oppdatertVilkaarsvurdering?.resultat?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaarsvurdering?.resultat?.saksbehandler)
            assertNotNull(oppdatertVilkaarsvurdering?.resultat?.tidspunkt)

            val sletteResponse = client.delete("/api/vilkaarsvurdering/resultat/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val slettetVilkaarsvurdering = objectMapper
                .readValue(sletteResponse.bodyAsText(), Vilkaarsvurdering::class.java)

            assertEquals(HttpStatusCode.OK, sletteResponse.status)
            assertEquals(behandlingId, slettetVilkaarsvurdering.behandlingId)
            assertEquals(null, slettetVilkaarsvurdering?.resultat)
        }
    }

    private fun opprettVilkaarsvurdering() {
        vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(
            behandlingId,
            SakType.BARNEPENSJON,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            "some payload",
            grunnlag
        )
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        val grunnlag: Grunnlag = objectMapper.readValue(readFile("/grunnlag.json"))
        const val ISSUER_ID = "azure"
        const val CLIENT_ID = "azure-id for saksbehandler"

        @Suppress("SameParameterValue")
        private fun readFile(file: String) = GrunnlagEndretRiverTest::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}