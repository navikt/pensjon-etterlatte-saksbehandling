package no.nav.etterlatte.vilkaarsvurdering

import GrunnlagTestData
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
import no.nav.etterlatte.libs.common.behandling.BehandlingType
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingRoutesTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private val server = MockOAuth2Server()

    private lateinit var vilkaarsvurderingServiceImpl: VilkaarsvurderingService
    private lateinit var applicationContext: ApplicationContext

    @BeforeAll
    fun before() {
        server.start()
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", server.wellKnownUrl(ISSUER_ID).toString())
        System.setProperty("AZURE_APP_CLIENT_ID", CLIENT_ID)
        postgreSQLContainer.start()

        applicationContext = ApplicationContext(
            ApplicationProperties(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password
            )
        )
        vilkaarsvurderingServiceImpl = applicationContext.vilkaarsvurderingService
    }

    @AfterAll
    fun after() {
        server.shutdown()
        postgreSQLContainer.stop()
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
            assertEquals(VilkaarType.DOEDSFALL_FORELDER, vilkaar.hovedvilkaar.type)
            assertEquals("§ 18-4", vilkaar.hovedvilkaar.paragraf.paragraf)
            assertEquals("Dødsfall forelder", vilkaar.hovedvilkaar.paragraf.tittel)
            assertEquals(
                "Barnepensjon ytes dersom en av foreldrene eller begge er døde. Bestemmelsen i § 17-2 andre " +
                    "ledd om forsvunnet ektefelle gjelder tilsvarende.",
                vilkaar.hovedvilkaar.paragraf.lovtekst
            )
            assertEquals("https://lovdata.no/lov/1997-02-28-19/%C2%A718-4", vilkaar.hovedvilkaar.paragraf.lenke)
            assertNull(vilkaar.vurdering)
        }
    }

    @Test
    fun `skal oppdatere en vilkaarsvurdering med et vurdert hovedvilkaar`() {
        testApplication {
            application { restModule(applicationContext) }

            opprettVilkaarsvurdering()

            val vurdertVilkaarDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfall(
                    VilkaarType.DOEDSFALL_FORELDER,
                    Utfall.OPPFYLT
                ),
                unntaksvilkaar = null,
                kommentar = "Søker oppfyller vilkåret"
            )

            val oppdatertVilkaarsvurderingResponse = client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val oppdatertVilkaarsvurdering = objectMapper
                .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), Vilkaarsvurdering::class.java)
            val oppdatertVilkaar = oppdatertVilkaarsvurdering.vilkaar.find {
                it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type
            }

            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(vurdertVilkaarDto.hovedvilkaar.type, oppdatertVilkaar?.hovedvilkaar?.type)
            assertEquals(vurdertVilkaarDto.hovedvilkaar.resultat, oppdatertVilkaar?.hovedvilkaar?.resultat)
            assertEquals(vurdertVilkaarDto.kommentar, oppdatertVilkaar?.vurdering?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaar?.vurdering?.saksbehandler)
            assertNotNull(oppdatertVilkaar?.vurdering?.tidspunkt)
        }
    }

    @Test
    fun `skal opprette vurdering paa hovedvilkaar og endre til vurdering paa unntaksvilkaar`() {
        testApplication {
            application { restModule(applicationContext) }

            opprettVilkaarsvurdering()

            val vurdertVilkaarDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP,
                    resultat = Utfall.OPPFYLT
                ),
                kommentar = "Søker oppfyller hovedvilkåret ${VilkaarType.FORUTGAAENDE_MEDLEMSKAP}"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)?.vilkaar
                ?.first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar?.vurdering)
            assertNotNull(vurdertVilkaar?.hovedvilkaar?.resultat)
            assertEquals(Utfall.OPPFYLT, vurdertVilkaar?.hovedvilkaar?.resultat)

            val vurdertVilkaarMedUnntakDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP,
                    resultat = Utfall.IKKE_OPPFYLT
                ),
                unntaksvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
                    resultat = Utfall.OPPFYLT
                ),
                kommentar = "Søker oppfyller unntaksvilkåret " +
                    "${VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR}"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarMedUnntakDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaarPaaUnntak = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)?.vilkaar
                ?.first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertEquals(Utfall.IKKE_OPPFYLT, vurdertVilkaarPaaUnntak?.hovedvilkaar?.resultat)
            assertNotNull(vurdertVilkaarPaaUnntak?.vurdering)
            assertNotNull(vurdertVilkaarPaaUnntak?.unntaksvilkaar)
            vurdertVilkaarPaaUnntak?.unntaksvilkaar?.forEach {
                if (it.type === VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR) {
                    assertEquals(Utfall.OPPFYLT, it.resultat)
                } else {
                    assertNull(it.resultat)
                }
            }
        }
    }

    @Test
    fun `skal nullstille et vurdert hovedvilkaar fra vilkaarsvurdering`() {
        testApplication {
            application { restModule(applicationContext) }

            opprettVilkaarsvurdering()

            val vurdertVilkaarDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.DOEDSFALL_FORELDER,
                    resultat = Utfall.OPPFYLT
                ),
                kommentar = "Søker oppfyller vilkåret"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)?.vilkaar
                ?.first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar?.vurdering)
            assertNotNull(vurdertVilkaar?.hovedvilkaar?.resultat)

            val response = client
                .delete("/api/vilkaarsvurdering/$behandlingId/${vurdertVilkaarDto.hovedvilkaar.type}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vurdertVilkaarSlettet = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)?.vilkaar
                ?.first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(vurdertVilkaarSlettet?.vurdering)
            assertNull(vurdertVilkaarSlettet?.hovedvilkaar?.resultat)
            vurdertVilkaarSlettet?.unntaksvilkaar?.forEach {
                assertNull(it.resultat)
            }
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
            objectMapper.createObjectNode(),
            grunnlag
        )
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        const val ISSUER_ID = "azure"
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}