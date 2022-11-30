package no.nav.etterlatte.vilkaarsvurdering

import GrunnlagTestData
import behandling.VirkningstidspunktTestData
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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.common.vilkaarsvurdering.UtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarTypeOgUtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaarDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.config.DataSourceBuilder
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlient
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.VilkaarType
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
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
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

    private lateinit var vilkaarsvurderingService: VilkaarsvurderingService
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

    @BeforeAll
    fun before() {
        server.start()
        System.setProperty("AZURE_APP_WELL_KNOWN_URL", server.wellKnownUrl(ISSUER_ID).toString())
        System.setProperty("AZURE_APP_CLIENT_ID", CLIENT_ID)
        postgreSQLContainer.start()

        val ds = DataSourceBuilder(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).apply { migrate() }

        vilkaarsvurderingService =
            VilkaarsvurderingService(
                VilkaarsvurderingRepositoryImpl(ds.dataSource()),
                behandlingKlient,
                grunnlagKlient,
                sendToRapid
            )

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery {
            grunnlagKlient.hentGrunnlagMedVersjon(
                any(),
                any(),
                any()
            )
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()
    }

    @AfterAll
    fun after() {
        server.shutdown()
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal hente vilkaarsvurdering`() {
        testApplication {
            application { restModule { vilkaarsvurdering(vilkaarsvurderingService) } }

            opprettVilkaarsvurdering()

            val response = client.get("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)
            val foersteVilkaar = vilkaarsvurdering.vilkaar.first()

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(behandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(VilkaarType.DOEDSFALL_FORELDER.name, foersteVilkaar.hovedvilkaar.type.name)
            assertEquals("§ 18-4", foersteVilkaar.hovedvilkaar.lovreferanse.paragraf)
            assertEquals("Dødsfall forelder", foersteVilkaar.hovedvilkaar.tittel)
            assertEquals(
                "En eller begge foreldrene er registrert død",
                foersteVilkaar.hovedvilkaar.beskrivelse
            )
            assertEquals(
                "https://lovdata.no/lov/1997-02-28-19/%C2%A718-4",
                foersteVilkaar.hovedvilkaar.lovreferanse.lenke
            )
            assertNull(foersteVilkaar.vurdering)
        }
    }

    @Test
    fun `skal opprette vilkaarsvurdering basert paa behandling dersom en ikke finnes`() {
        testApplication {
            application { restModule { vilkaarsvurdering(vilkaarsvurderingService) } }

            val nyBehandlingId = UUID.randomUUID()
            val response = client.get("/api/vilkaarsvurdering/$nyBehandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)

            assertEquals(nyBehandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(
                vilkaarsvurdering.virkningstidspunkt.dato,
                VirkningstidspunktTestData.virkningstidsunkt().dato
            )
            assertNull(vilkaarsvurdering.resultat)
        }
    }

    @Test
    fun `skal kaste feil dersom virkningstidspunkt ikke finnes`() {
        testApplication {
            application { restModule { vilkaarsvurdering(vilkaarsvurderingService) } }
            val nyBehandlingId = UUID.randomUUID()

            coEvery { behandlingKlient.hentBehandling(nyBehandlingId, any()) } returns detaljertBehandling().apply {
                every { virkningstidspunkt } returns null
            }

            val response = client.get("/api/vilkaarsvurdering/$nyBehandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.PreconditionFailed, response.status)
        }
    }

    @Test
    fun `skal oppdatere en vilkaarsvurdering med et vurdert hovedvilkaar`() {
        testApplication {
            application { restModule { vilkaarsvurdering(vilkaarsvurderingService) } }

            opprettVilkaarsvurdering()

            val vurdertVilkaarDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfallDto(
                    VilkaarTypeDto.DOEDSFALL_FORELDER,
                    UtfallDto.OPPFYLT
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
                .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            val oppdatertVilkaar = oppdatertVilkaarsvurdering.vilkaar.find {
                it.hovedvilkaar.type.name == vurdertVilkaarDto.hovedvilkaar.type.name
            }

            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(vurdertVilkaarDto.hovedvilkaar.type.name, oppdatertVilkaar?.hovedvilkaar?.type?.name)
            assertEquals(vurdertVilkaarDto.hovedvilkaar.resultat.name, oppdatertVilkaar?.hovedvilkaar?.resultat?.name)
            assertEquals(vurdertVilkaarDto.kommentar, oppdatertVilkaar?.vurdering?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaar?.vurdering?.saksbehandler)
            assertNotNull(oppdatertVilkaar?.vurdering?.tidspunkt)
        }
    }

    @Test
    fun `skal opprette vurdering paa hovedvilkaar og endre til vurdering paa unntaksvilkaar`() {
        testApplication {
            application { restModule { vilkaarsvurdering(vilkaarsvurderingService) } }

            opprettVilkaarsvurdering()

            val vurdertVilkaarDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfallDto(
                    type = VilkaarTypeDto.FORUTGAAENDE_MEDLEMSKAP,
                    resultat = UtfallDto.OPPFYLT
                ),
                kommentar = "Søker oppfyller hovedvilkåret ${VilkaarType.FORUTGAAENDE_MEDLEMSKAP}"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar = vilkaarsvurderingService.hentEllerOpprettVilkaarsvurdering(behandlingId, oboToken)
                .vilkaar.first { it.hovedvilkaar.type.name == vurdertVilkaarDto.hovedvilkaar.type.name }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar.vurdering)
            assertNotNull(vurdertVilkaar.hovedvilkaar.resultat)
            assertEquals(Utfall.OPPFYLT, vurdertVilkaar.hovedvilkaar.resultat)

            val vurdertVilkaarMedUnntakDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfallDto(
                    type = VilkaarTypeDto.FORUTGAAENDE_MEDLEMSKAP,
                    resultat = UtfallDto.IKKE_OPPFYLT
                ),
                unntaksvilkaar = VilkaarTypeOgUtfallDto(
                    type = VilkaarTypeDto.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
                    resultat = UtfallDto.OPPFYLT
                ),
                kommentar = "Søker oppfyller unntaksvilkåret " +
                    "${VilkaarType.FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR}"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarMedUnntakDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaarPaaUnntak = vilkaarsvurderingService.hentEllerOpprettVilkaarsvurdering(
                behandlingId,
                oboToken
            )
                .vilkaar.first { it.hovedvilkaar.type.name == vurdertVilkaarDto.hovedvilkaar.type.name }

            assertEquals(Utfall.IKKE_OPPFYLT, vurdertVilkaarPaaUnntak.hovedvilkaar.resultat)
            assertNotNull(vurdertVilkaarPaaUnntak.vurdering)
            assertNotNull(vurdertVilkaarPaaUnntak.unntaksvilkaar)
            vurdertVilkaarPaaUnntak.unntaksvilkaar?.forEach {
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
            application { restModule { vilkaarsvurdering(vilkaarsvurderingService) } }

            opprettVilkaarsvurdering()

            val vurdertVilkaarDto = VurdertVilkaarDto(
                hovedvilkaar = VilkaarTypeOgUtfallDto(
                    type = VilkaarTypeDto.DOEDSFALL_FORELDER,
                    resultat = UtfallDto.OPPFYLT
                ),
                kommentar = "Søker oppfyller vilkåret"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar = vilkaarsvurderingService.hentEllerOpprettVilkaarsvurdering(behandlingId, oboToken)
                .vilkaar.first { it.hovedvilkaar.type.name == vurdertVilkaarDto.hovedvilkaar.type.name }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar.vurdering)
            assertNotNull(vurdertVilkaar.hovedvilkaar.resultat)

            val response = client
                .delete("/api/vilkaarsvurdering/$behandlingId/${vurdertVilkaarDto.hovedvilkaar.type}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vurdertVilkaarSlettet = vilkaarsvurderingService.hentEllerOpprettVilkaarsvurdering(
                behandlingId,
                oboToken
            ).vilkaar
                .first { it.hovedvilkaar.type.name == vurdertVilkaarDto.hovedvilkaar.type.name }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(vurdertVilkaarSlettet.vurdering)
            assertNull(vurdertVilkaarSlettet.hovedvilkaar.resultat)
            vurdertVilkaarSlettet.unntaksvilkaar?.forEach {
                assertNull(it.resultat)
            }
        }
    }

    @Test
    fun `skal sette og nullstille totalresultat for en vilkaarsvurdering`() {
        testApplication {
            application { restModule { vilkaarsvurdering(vilkaarsvurderingService) } }

            opprettVilkaarsvurdering()
            val resultat = VurdertVilkaarsvurderingResultatDto(
                resultat = VilkaarsvurderingUtfallDto.OPPFYLT,
                kommentar = "Søker oppfyller vurderingen"
            )
            val oppdatertVilkaarsvurderingResponse = client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                setBody(resultat.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val oppdatertVilkaarsvurdering = objectMapper
                .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)

            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(resultat.resultat.toString(), oppdatertVilkaarsvurdering?.resultat?.utfall.toString())
            assertEquals(resultat.kommentar, oppdatertVilkaarsvurdering?.resultat?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaarsvurdering?.resultat?.saksbehandler)
            assertNotNull(oppdatertVilkaarsvurdering?.resultat?.tidspunkt)
            verify(exactly = 1) { sendToRapid.invoke(any(), any()) }

            val sletteResponse = client.delete("/api/vilkaarsvurdering/resultat/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val slettetVilkaarsvurdering = objectMapper
                .readValue(sletteResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, sletteResponse.status)
            assertEquals(behandlingId, slettetVilkaarsvurdering.behandlingId)
            assertEquals(null, slettetVilkaarsvurdering?.resultat)
        }
    }

    private fun opprettVilkaarsvurdering() {
        runBlocking {
            vilkaarsvurderingService.hentEllerOpprettVilkaarsvurdering(
                behandlingId,
                oboToken
            )
        }
    }

    private fun detaljertBehandling() = mockk<DetaljertBehandling>().apply {
        every { id } returns UUID.randomUUID()
        every { sak } returns 1L
        every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
        every { soeker } returns "10095512345"
        every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
        every { revurderingsaarsak } returns null
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        val oboToken = "token"
        const val ISSUER_ID = "azure"
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}