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
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingRoutesTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private val server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()

    private lateinit var vilkaarsvurderingServiceImpl: VilkaarsvurderingService
    private lateinit var ds: DataSource

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, CLIENT_ID)
        postgreSQLContainer.start()
        ds = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }

        vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(VilkaarsvurderingRepository(ds), behandlingKlient, grunnlagKlient)

        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery {
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any(), VilkaarsvurderingUtfall.OPPFYLT)
        } returns true
        coEvery { behandlingKlient.settBehandlingStatusOpprettet(any(), any(), any()) } returns true
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
    }

    @AfterEach
    fun afterEach() {
        ds.connection.use {
            it.prepareStatement("TRUNCATE vilkaarsvurdering CASCADE;").execute()
        }
    }

    @AfterAll
    fun after() {
        server.shutdown()
        postgreSQLContainer.stop()
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

    @Test
    fun `skal hente vilkaarsvurdering`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val response = client.get("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vilkaarsvurdering = objectMapper.readValue(response.bodyAsText(), VilkaarsvurderingDto::class.java)
            val vilkaar = vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.BP_DOEDSFALL_FORELDER }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(behandlingId, vilkaarsvurdering.behandlingId)
            assertEquals(VilkaarType.BP_DOEDSFALL_FORELDER, vilkaar.hovedvilkaar.type)
            assertEquals("§ 18-4", vilkaar.hovedvilkaar.lovreferanse.paragraf)
            assertEquals("Dødsfall forelder", vilkaar.hovedvilkaar.tittel)
            assertEquals(
                """
                    For å ha rett på ytelsen må en eller begge foreldre være registrer død i folkeregisteret eller hos utenlandske myndigheter.
                """.trimIndent(),
                vilkaar.hovedvilkaar.beskrivelse
            )
            assertEquals("https://lovdata.no/lov/1997-02-28-19/%C2%A718-4", vilkaar.hovedvilkaar.lovreferanse.lenke)
            assertNull(vilkaar.vurdering)
        }
    }

    @Test
    fun `skal returnere no content dersom en vilkaarsvurdering ikke finnes`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val nyBehandlingId = UUID.randomUUID()
            val response = client.get("/api/vilkaarsvurdering/$nyBehandlingId") {
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
        coEvery { behandlingKlient.harTilgangTilBehandling(nyBehandlingId, any()) } returns false

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val response = client.get("/api/vilkaarsvurdering/$nyBehandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(response.status, HttpStatusCode.NotFound)
            coVerify(exactly = 1) {
                behandlingKlient.harTilgangTilBehandling(nyBehandlingId, any())
            }
        }
    }

    @Test
    fun `skal kaste feil dersom virkningstidspunkt ikke finnes ved opprettelse`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }
            val nyBehandlingId = UUID.randomUUID()

            coEvery { behandlingKlient.hentBehandling(nyBehandlingId, any()) } returns detaljertBehandling().apply {
                every { virkningstidspunkt } returns null
            }

            val response = client.post("/api/vilkaarsvurdering/$nyBehandlingId/opprett") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.PreconditionFailed, response.status)
        }
    }

    @Test
    fun `skal oppdatere en vilkaarsvurdering med et vurdert hovedvilkaar`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val vilkaarsvurdering = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto = VurdertVilkaarDto(
                vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER)?.id!!,
                hovedvilkaar = VilkaarTypeOgUtfall(
                    VilkaarType.BP_DOEDSFALL_FORELDER,
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
                .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
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
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val vilkaarsvurdering = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto = VurdertVilkaarDto(
                vilkaarId = vilkaarsvurdering
                    .hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP)?.id!!,
                hovedvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP,
                    resultat = Utfall.OPPFYLT
                ),
                kommentar = "Søker oppfyller hovedvilkåret ${VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP}"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
                .vilkaar.first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar.vurdering)
            assertNotNull(vurdertVilkaar.hovedvilkaar.resultat)
            assertEquals(Utfall.OPPFYLT, vurdertVilkaar.hovedvilkaar.resultat)

            val vurdertVilkaarMedUnntakDto = VurdertVilkaarDto(
                vilkaarId = vilkaarsvurdering
                    .hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP)?.id!!,
                hovedvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP,
                    resultat = Utfall.IKKE_OPPFYLT
                ),
                unntaksvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR,
                    resultat = Utfall.OPPFYLT
                ),
                kommentar = "Søker oppfyller unntaksvilkåret " +
                    "${VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR}"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarMedUnntakDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaarPaaUnntak = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
                .vilkaar.first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertEquals(Utfall.IKKE_OPPFYLT, vurdertVilkaarPaaUnntak.hovedvilkaar.resultat)
            assertNotNull(vurdertVilkaarPaaUnntak.vurdering)
            assertNotNull(vurdertVilkaarPaaUnntak.unntaksvilkaar)
            vurdertVilkaarPaaUnntak.unntaksvilkaar.forEach {
                if (it.type === VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP_UNNTAK_AVDOED_IKKE_FYLT_26_AAR) {
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
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val vilkaarsvurdering = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto = VurdertVilkaarDto(
                vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER)?.id!!,
                hovedvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.BP_DOEDSFALL_FORELDER,
                    resultat = Utfall.OPPFYLT
                ),
                kommentar = "Søker oppfyller vilkåret"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            val vurdertVilkaar = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!
                .vilkaar.first { it.hovedvilkaar.type == vurdertVilkaarDto.hovedvilkaar.type }

            assertNotNull(vurdertVilkaar)
            assertNotNull(vurdertVilkaar.vurdering)
            assertNotNull(vurdertVilkaar.hovedvilkaar.resultat)

            val response = client
                .delete("/api/vilkaarsvurdering/$behandlingId/${vurdertVilkaarDto.vilkaarId}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val vurdertVilkaarSlettet = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)!!.vilkaar
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
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)
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
                .readValue(oppdatertVilkaarsvurderingResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
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
                .readValue(sletteResponse.bodyAsText(), VilkaarsvurderingDto::class.java)
            assertEquals(HttpStatusCode.OK, sletteResponse.status)
            assertEquals(behandlingId, slettetVilkaarsvurdering.behandlingId)
            assertEquals(null, slettetVilkaarsvurdering?.resultat)
        }
    }

    @Test
    fun `skal ikke kunne endre eller slette vilkaar naar totalresultat er satt`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val vilkaarsvurdering = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)
            val resultat = VurdertVilkaarsvurderingResultatDto(
                resultat = VilkaarsvurderingUtfall.OPPFYLT,
                kommentar = "Søker oppfyller vurderingen"
            )
            client.post("/api/vilkaarsvurdering/resultat/$behandlingId") {
                setBody(resultat.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val vurdertVilkaarDto = VurdertVilkaarDto(
                vilkaarId = vilkaarsvurdering.hentVilkaarMedHovedvilkaarType(VilkaarType.BP_DOEDSFALL_FORELDER)?.id!!,
                hovedvilkaar = VilkaarTypeOgUtfall(
                    VilkaarType.BP_DOEDSFALL_FORELDER,
                    Utfall.OPPFYLT
                ),
                unntaksvilkaar = null,
                kommentar = "Søker oppfyller vilkåret"
            )

            client.post("/api/vilkaarsvurdering/$behandlingId") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let { assertEquals(HttpStatusCode.PreconditionFailed, it.status) }
            client.delete("/api/vilkaarsvurdering/$behandlingId/${vurdertVilkaarDto.vilkaarId}") {
                setBody(vurdertVilkaarDto.toJson())
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let { assertEquals(HttpStatusCode.PreconditionFailed, it.status) }
        }
    }

    @Test
    fun `faar 401 hvis spoerring ikke har access token`() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }
            val response = client.get("/api/vilkaarsvurdering/$behandlingId")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `statussjekk kalles paa en gang for aa sjekke tilstanden paa behandling`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(VilkaarsvurderingRepository(ds), behandlingKlient, grunnlagKlient)

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }
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
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(VilkaarsvurderingRepository(ds), behandlingKlient, grunnlagKlient)

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val response = client.post("/api/vilkaarsvurdering/$behandlingId/opprett") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            assertEquals(HttpStatusCode.PreconditionFailed, response.status)
            coVerify(exactly = 0) { behandlingKlient.settBehandlingStatusVilkaarsvurdert(any(), any(), any()) }
        }
    }

    @Test
    fun `skal ikke commite vilkaarsvurdering hvis statussjekk feiler ved sletting av totalvurdering`() {
        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.settBehandlingStatusOpprettet(any(), any(), any()) } returns false
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(VilkaarsvurderingRepository(ds), behandlingKlient, grunnlagKlient)

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

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
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returnsMany listOf(
            true,
            false
        )
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true

        val vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(VilkaarsvurderingRepository(ds), behandlingKlient, grunnlagKlient)

        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient) } }

            val vilkaarsvurdering = opprettVilkaarsvurdering(vilkaarsvurderingServiceImpl)

            val vurdertVilkaarDto = VurdertVilkaarDto(
                vilkaarId = vilkaarsvurdering
                    .hentVilkaarMedHovedvilkaarType(VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP)?.id!!,
                hovedvilkaar = VilkaarTypeOgUtfall(
                    type = VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP,
                    resultat = Utfall.OPPFYLT
                ),
                kommentar = "Søker oppfyller hovedvilkåret ${VilkaarType.BP_FORUTGAAENDE_MEDLEMSKAP}"
            )

            val response = client.post("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, "application/json")
                setBody(vurdertVilkaarDto.toJson())
            }

            assertEquals(HttpStatusCode.PreconditionFailed, response.status)
            val actual = vilkaarsvurderingServiceImpl.hentVilkaarsvurdering(behandlingId)
            assertEquals(vilkaarsvurdering, actual)
        }
    }

    private fun opprettVilkaarsvurdering(vilkaarsvurderingService: VilkaarsvurderingService): Vilkaarsvurdering =
        runBlocking {
            vilkaarsvurderingService.opprettVilkaarsvurdering(behandlingId, oboToken)
        }

    private fun detaljertBehandling() = mockk<DetaljertBehandling>().apply {
        every { id } returns UUID.randomUUID()
        every { sak } returns 1L
        every { sakType } returns SakType.BARNEPENSJON
        every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
        every { soeker } returns "10095512345"
        every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
        every { revurderingsaarsak } returns null
    }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        val oboToken = Bruker.of("token", "s1", null, null)
        const val CLIENT_ID = "azure-id for saksbehandler"
    }
}