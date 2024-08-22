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
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingRoutesTest(
    private val ds: DataSource,
) {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val grunnlagVersjon = 12L
    private val nyGrunnlagVersjon: Long = 4378

    private lateinit var vilkaarsvurderingServiceImpl: VilkaarsvurderingService

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()

        vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                VilkaarsvurderingRepository(ds, DelvilkaarRepository()),
                behandlingKlient,
                grunnlagKlient,
            )
    }

    @BeforeEach
    fun beforeEach() {
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery {
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any())
        } returns true
        coEvery { behandlingKlient.settBehandlingStatusOpprettet(any(), any(), any()) } returns true
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
        val grunnlagMock = grunnlagMedVersjon(grunnlagVersjon)
        coEvery { grunnlagKlient.hentGrunnlagForBehandling(any(), any()) } returns grunnlagMock
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    @Test
    fun `skal hente vilkaarsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val response =
                client.get("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)
            val vilkaar = vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.BP_DOEDSFALL_FORELDER_2024 }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(behandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(grunnlagVersjon, vilkaarsvurdering.grunnlagVersjon)
            assertEquals(grunnlagVersjon, vilkaarsvurdering.behandlingGrunnlagVersjon)
            assertFalse(vilkaarsvurdering.isGrunnlagUtdatert())

            assertEquals(VilkaarType.BP_DOEDSFALL_FORELDER_2024, vilkaar.hovedvilkaar.type)
            assertEquals("§ 18-1", vilkaar.hovedvilkaar.lovreferanse.paragraf)
            assertEquals("Dødsfall forelder", vilkaar.hovedvilkaar.tittel)
            assertEquals(
                """
                For å ha rett på ytelsen må en eller begge foreldre være registrer død i folkeregisteret eller hos utenlandske myndigheter.
                """.trimIndent(),
                vilkaar.hovedvilkaar.beskrivelse,
            )
            assertEquals("https://lovdata.no/lov/1997-02-28-19/%C2%A718-1", vilkaar.hovedvilkaar.lovreferanse.lenke)
            assertNull(vilkaar.vurdering)
        }
    }

    @Test
    fun `skal hente vilkaarsvurdering med ny versjon på behandlingens grunnlag`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            coEvery { grunnlagKlient.hentGrunnlagForBehandling(behandlingId, any()) } returns
                grunnlagMedVersjon(nyGrunnlagVersjon)

            val response =
                client.get("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(behandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(grunnlagVersjon, vilkaarsvurdering.grunnlagVersjon)
            assertEquals(nyGrunnlagVersjon, vilkaarsvurdering.behandlingGrunnlagVersjon)
            assertTrue(vilkaarsvurdering.isGrunnlagUtdatert())
        }
    }

    @Test
    fun `skal returnere no content dersom en vilkaarsvurdering ikke finnes`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            val nyBehandlingId = UUID.randomUUID()
            val response =
                client.get("/api/vilkaarsvurdering/$nyBehandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(response.status, HttpStatusCode.NoContent)
        }
    }

    @Test
    fun `skal returnere not found dersom saksbehandler ikke har tilgang til behandlingen`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        val nyBehandlingId = UUID.randomUUID()
        coEvery { behandlingKlient.harTilgangTilBehandling(nyBehandlingId, any(), any()) } returns false

        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }
            val response =
                client.get("/api/vilkaarsvurdering/$nyBehandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(response.status, HttpStatusCode.NotFound)
            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(nyBehandlingId, any(), any())
            }
        }
    }

    @Test
    fun `skal kaste feil dersom virkningstidspunkt ikke finnes ved opprettelse`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }
            val nyBehandlingId = UUID.randomUUID()

            coEvery { behandlingKlient.hentBehandling(nyBehandlingId, any()) } returns
                detaljertBehandling().apply {
                    every { virkningstidspunkt } returns null
                }

            val response =
                client.post("/api/vilkaarsvurdering/$nyBehandlingId/opprett") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(HttpStatusCode.PreconditionFailed, response.status)
        }
    }

    @Test
    fun `skal oppdatere en vilkaarsvurdering med et vurdert hovedvilkaar`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            val (vilkaarsvurdering) = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto =
                VurdertVilkaarDto(
                    vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER_2024)?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                            Utfall.OPPFYLT,
                        ),
                    unntaksvilkaar = null,
                    kommentar = "Søker oppfyller vilkåret",
                )

            val oppdatertVilkaarsvurderingResponse =
                client.post("/api/vilkaarsvurdering/$behandlingId") {
                    setBody(vurdertVilkaarDto.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val oppdatertVilkaarsvurdering =
                objectMapper
                    .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            val oppdatertVilkaar =
                oppdatertVilkaarsvurdering.vilkaar.find {
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
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            val (vilkaarsvurdering) = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto =
                VurdertVilkaarDto(
                    vilkaarId =
                        vilkaarsvurdering
                            .hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024)
                            ?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024,
                            resultat = Utfall.OPPFYLT,
                        ),
                    kommentar = "Søker oppfyller hovedvilkåret ${VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024}",
                )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar =
                vilkaarsvurderingServiceImpl
                    .hentVilkaarsvurdering(behandlingId)!!
                    .vilkaar
                    .first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar.vurdering)
            assertNotNull(vurdertVilkaar.hovedvilkaar.resultat)
            assertEquals(Utfall.OPPFYLT, vurdertVilkaar.hovedvilkaar.resultat)

            val vurdertVilkaarMedUnntakDto =
                VurdertVilkaarDto(
                    vilkaarId =
                        vilkaarsvurdering
                            .hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024)
                            ?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024,
                            resultat = Utfall.IKKE_OPPFYLT,
                        ),
                    unntaksvilkaar =
                        VilkaarTypeOgUtfall(
                            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR_2024,
                            resultat = Utfall.OPPFYLT,
                        ),
                    kommentar =
                        "Søker oppfyller unntaksvilkåret " +
                            "${VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR_2024}",
                )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarMedUnntakDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaarPaaUnntak =
                vilkaarsvurderingServiceImpl
                    .hentVilkaarsvurdering(behandlingId)!!
                    .vilkaar
                    .first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertEquals(Utfall.IKKE_OPPFYLT, vurdertVilkaarPaaUnntak.hovedvilkaar.resultat)
            assertNotNull(vurdertVilkaarPaaUnntak.vurdering)
            assertNotNull(vurdertVilkaarPaaUnntak.unntaksvilkaar)
            vurdertVilkaarPaaUnntak.unntaksvilkaar.forEach {
                if (it.type === VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR_2024) {
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
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            val (vilkaarsvurdering) = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto =
                VurdertVilkaarDto(
                    vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER_2024)?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            type = VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                            resultat = Utfall.OPPFYLT,
                        ),
                    kommentar = "Søker oppfyller vilkåret",
                )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar =
                vilkaarsvurderingServiceImpl
                    .hentVilkaarsvurdering(behandlingId)!!
                    .vilkaar
                    .first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar.vurdering)
            assertNotNull(vurdertVilkaar.hovedvilkaar.resultat)

            val response =
                client
                    .delete("/api/vilkaarsvurdering/$behandlingId/${vurdertVilkaarDto.vilkaarId}") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }

            val vurdertVilkaarSlettet =
                vilkaarsvurderingServiceImpl
                    .hentVilkaarsvurdering(behandlingId)!!
                    .vilkaar
                    .first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(vurdertVilkaarSlettet.vurdering)
            assertNull(vurdertVilkaarSlettet.hovedvilkaar.resultat)
            vurdertVilkaarSlettet.unntaksvilkaar.forEach {
                assertNull(it.resultat)
            }
        }
    }

    @Test
    fun `skal sette og nullstille totalresultat for en vilkaarsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)
            val resultat =
                VurdertVilkaarsvurderingResultatDto(
                    resultat = VilkaarsvurderingUtfall.OPPFYLT,
                    kommentar = "Søker oppfyller vurderingen",
                )
            val oppdatertVilkaarsvurderingResponse =
                client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                    setBody(resultat.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val oppdatertVilkaarsvurdering =
                objectMapper
                    .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, oppdatertVilkaarsvurderingResponse.status)
            assertEquals(behandlingId, oppdatertVilkaarsvurdering.behandlingId)
            assertEquals(resultat.resultat, oppdatertVilkaarsvurdering?.resultat?.utfall)
            assertEquals(resultat.kommentar, oppdatertVilkaarsvurdering?.resultat?.kommentar)
            assertEquals("Saksbehandler01", oppdatertVilkaarsvurdering?.resultat?.saksbehandler)
            assertNotNull(oppdatertVilkaarsvurdering?.resultat?.tidspunkt)

            val sletteResponse =
                client.delete("/api/vilkaarsvurdering/resultat/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val slettetVilkaarsvurdering =
                objectMapper
                    .readValue(sletteResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, sletteResponse.status)
            assertEquals(behandlingId, slettetVilkaarsvurdering.behandlingId)
            assertEquals(null, slettetVilkaarsvurdering?.resultat)
        }
    }

    @Test
    fun `skal ikke kunne endre eller slette vilkaar naar totalresultat er satt`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            val (vilkaarsvurdering) = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)
            val resultat =
                VurdertVilkaarsvurderingResultatDto(
                    resultat = VilkaarsvurderingUtfall.OPPFYLT,
                    kommentar = "Søker oppfyller vurderingen",
                )
            client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                setBody(resultat.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val vurdertVilkaarDto =
                VurdertVilkaarDto(
                    vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER_2024)?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                            Utfall.OPPFYLT,
                        ),
                    unntaksvilkaar = null,
                    kommentar = "Søker oppfyller vilkåret",
                )

            client
                .post("/api/vilkaarsvurdering/$behandlingId") {
                    setBody(vurdertVilkaarDto.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { assertEquals(HttpStatusCode.PreconditionFailed, it.status) }
            client
                .delete("/api/vilkaarsvurdering/$behandlingId/${vurdertVilkaarDto.vilkaarId}") {
                    setBody(vurdertVilkaarDto.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { assertEquals(HttpStatusCode.PreconditionFailed, it.status) }
        }
    }

    @Test
    fun `revurdering skal kopiere siste vilkaarsvurdering ved opprettele som default`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)
            val resultat =
                VurdertVilkaarsvurderingResultatDto(
                    resultat = VilkaarsvurderingUtfall.OPPFYLT,
                    kommentar = "Søker oppfyller vurderingen",
                )
            client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                setBody(resultat.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val revurderingBehandlingId = UUID.randomUUID()
            coEvery { behandlingKlient.hentBehandling(revurderingBehandlingId, any()) } returns
                detaljertBehandling().apply {
                    every { behandlingType } returns BehandlingType.REVURDERING
                    every { id } returns revurderingBehandlingId
                }
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns SisteIverksatteBehandling(behandlingId)

            val response =
                client.post("/api/vilkaarsvurdering/$revurderingBehandlingId/opprett") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(revurderingBehandlingId, vilkaarsvurdering.behandlingId)
            assertNull(vilkaarsvurdering.resultat) // siden vilkår ikke har vurdering her
        }
    }

    @Test
    fun `revurdering skal ikke kopiere siste vilkaarsvurdering naar kopierVedRevurdering er false`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)
            val resultat =
                VurdertVilkaarsvurderingResultatDto(
                    resultat = VilkaarsvurderingUtfall.OPPFYLT,
                    kommentar = "Søker oppfyller vurderingen",
                )
            client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                setBody(resultat.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val revurderingBehandlingId = UUID.randomUUID()
            coEvery { behandlingKlient.hentBehandling(revurderingBehandlingId, any()) } returns
                detaljertBehandling().apply {
                    every { behandlingType } returns BehandlingType.REVURDERING
                    every { id } returns revurderingBehandlingId
                }

            val response =
                client.post("/api/vilkaarsvurdering/$revurderingBehandlingId/opprett?kopierVedRevurdering=false") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(revurderingBehandlingId, vilkaarsvurdering.behandlingId)
            assertTrue(vilkaarsvurdering.resultat == null)
        }
    }

    @Test
    fun `Skal slette eksisterende vilkaarsvurdering`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val response =
                client.delete("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("", response.bodyAsText())
        }
    }

    @Test
    fun `faar 401 hvis spoerring ikke har access token`() {
        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }
            val response = client.get("/api/vilkaarsvurdering/$behandlingId")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `statussjekk kalles paa en gang for aa sjekke tilstanden paa behandling`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                VilkaarsvurderingRepository(ds, DelvilkaarRepository()),
                behandlingKlient,
                grunnlagKlient,
            )

        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }
            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            coVerify(exactly = 1) {
                behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any())
            }
        }
    }

    @Test
    fun `kan ikke opprette eller committe vilkaarsvurdering hvis statussjekk feiler`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns false
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                VilkaarsvurderingRepository(ds, DelvilkaarRepository()),
                behandlingKlient,
                grunnlagKlient,
            )

        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            val response =
                client.post("/api/vilkaarsvurdering/$behandlingId/opprett") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(HttpStatusCode.PreconditionFailed, response.status)
            coVerify(exactly = 0) { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) }
        }
    }

    @Test
    fun `skal ikke commite vilkaarsvurdering hvis statussjekk feiler ved sletting av totalvurdering`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.settBehandlingStatusOpprettet(any(), any(), any()) } returns false
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                VilkaarsvurderingRepository(ds, DelvilkaarRepository()),
                behandlingKlient,
                grunnlagKlient,
            )

        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            client.delete("/api/vilkaarsvurdering/resultat/$behandlingId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        coVerify(exactly = 1) { behandlingKlient.settBehandlingStatusOpprettet(any(), any(), false) }
        coVerify(exactly = 0) { behandlingKlient.settBehandlingStatusOpprettet(any(), any(), true) }
    }

    @Test
    fun `kan ikke endre vilkaarsvurdering hvis statussjekk feiler`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returnsMany
            listOf(
                true,
                false,
            )
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                VilkaarsvurderingRepository(ds, DelvilkaarRepository()),
                behandlingKlient,
                grunnlagKlient,
            )

        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            val (vilkaarsvurdering) = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto =
                VurdertVilkaarDto(
                    vilkaarId =
                        vilkaarsvurdering
                            .hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024)
                            ?.id!!,
                    hovedvilkaar =
                        VilkaarTypeOgUtfall(
                            type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024,
                            resultat = Utfall.OPPFYLT,
                        ),
                    kommentar = "Søker oppfyller hovedvilkåret ${VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_2024}",
                )

            val response =
                client.post("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.ContentType, "application/json")
                    setBody(vurdertVilkaarDto.toJson())
                }

            assertEquals(HttpStatusCode.PreconditionFailed, response.status)
            val actual = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)
            assertEquals(vilkaarsvurdering, actual)
        }
    }

    @Test
    fun `skal sjekke gyldighet og oppdatere status hvis den er OPPRETTET`() {
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
            }

            runBlocking {
                vilkaarsvurderingServiceImpl.opprettVilkaarsvurdering(behandlingId, oboToken)
                vilkaarsvurderingServiceImpl.oppdaterTotalVurdering(behandlingId, oboToken, vilkaarsvurderingResultat())
            }

            val response =
                client.post("/api/vilkaarsvurdering/$behandlingId/oppdater-status") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val dto = objectMapper.readValue(response.bodyAsText(), StatusOppdatertDto::class.java)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(dto.statusOppdatert, true)

            coVerify(exactly = 2) {
                behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any())
            }
        }
    }

    private fun opprettVilkaarsvurdering(
        vilkaarsvurderingService: VilkaarsvurderingService,
    ): VilkaarsvurderingMedBehandlingGrunnlagsversjon =
        runBlocking {
            vilkaarsvurderingService.opprettVilkaarsvurdering(behandlingId, oboToken)
        }

    private fun detaljertBehandling() =
        mockk<DetaljertBehandling>().apply {
            every { id } returns UUID.randomUUID()
            every { sak } returns 1L
            every { sakType } returns SakType.BARNEPENSJON
            every { status } returns BehandlingStatus.OPPRETTET
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
            every { revurderingsaarsak } returns null
        }

    private fun vilkaarsvurderingResultat(utfall: VilkaarsvurderingUtfall = VilkaarsvurderingUtfall.OPPFYLT) =
        VilkaarsvurderingResultat(
            utfall = utfall,
            kommentar = "Kommentar",
            tidspunkt = LocalDateTime.now(),
            saksbehandler = "Saksbehandler",
        )

    private fun grunnlagMedVersjon(grunnlagVersjon: Long): Grunnlag {
        val grunnlagMock =
            mockk<Grunnlag> {
                every { metadata } returns
                    mockk {
                        every { versjon } returns grunnlagVersjon
                    }
            }
        return grunnlagMock
    }

    private companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()

        val behandlingId: UUID = UUID.randomUUID()
        val oboToken = simpleSaksbehandler()
    }
}
