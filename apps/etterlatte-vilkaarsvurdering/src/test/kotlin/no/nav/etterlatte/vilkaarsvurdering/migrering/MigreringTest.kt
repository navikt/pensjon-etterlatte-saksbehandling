package no.nav.etterlatte.vilkaarsvurdering.migrering

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ktor.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingMigreringRequest
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import no.nav.etterlatte.vilkaarsvurdering.vilkaarsvurdering
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private val server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private lateinit var ds: DataSource
    private lateinit var migreringService: MigreringService
    private lateinit var vilkaarsvurderingRepository: VilkaarsvurderingRepository
    private lateinit var vilkaarsvurderingServiceImpl: VilkaarsvurderingService

    @BeforeAll
    fun before() {
        server.start()
        postgreSQLContainer.start()
        ds =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }

        val delvilkaarRepository = DelvilkaarRepository()
        vilkaarsvurderingRepository = VilkaarsvurderingRepository(ds, delvilkaarRepository)

        val grunnlagKlient = mockk<GrunnlagKlient>()
        vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                vilkaarsvurderingRepository,
                behandlingKlient,
                grunnlagKlient,
            )

        migreringService =
            MigreringService(
                vilkaarsvurderingRepository,
            )
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(behandlingId, any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, any()) } returns true
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
    }

    private fun detaljertBehandling() =
        mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 1L
            every { sakType } returns SakType.BARNEPENSJON
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            every { soeker } returns "10095512345"
            every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1))
            every { revurderingsaarsak } returns null
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

    private val token: String by lazy { server.issueSaksbehandlerToken() }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
    }

    @Test
    fun `Skal opprette vilkaarsvurdering for migrering`() {
        testApplication {
            runServer(server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
                migrering(migreringService, behandlingKlient, vilkaarsvurderingServiceImpl)
            }

            val response =
                client.post("/api/vilkaarsvurdering/migrering/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(VilkaarsvurderingMigreringRequest(yrkesskadeFordel = false).toJson())
                }
            assertEquals(HttpStatusCode.Accepted, response.status)

            val vilkaar =
                client.get("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                    .also { assertTrue(it.status.isSuccess()) }
                    .let { objectMapper.readValue(it.bodyAsText(), VilkaarsvurderingDto::class.java) }
                    .vilkaar
                    .also { assertEquals(it.size, 8) }
            val vilkaarOppfylles =
                listOf(
                    VilkaarType.BP_FORMAAL_2024,
                    VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                    VilkaarType.BP_ALDER_BARN_2024,
                    VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                )
            val vilkarIkkeOppfylt = listOf(VilkaarType.BP_YRKESSKADE_AVDOED_2024)

            vilkaar.filter { vilkaarOppfylles.contains(it.hovedvilkaar.type) }
                .forEach { assertEquals(it.hovedvilkaar.resultat, Utfall.OPPFYLT) }

            vilkaar.filter { !(vilkaarOppfylles + vilkarIkkeOppfylt).contains(it.hovedvilkaar.type) }
                .forEach { assertEquals(it.hovedvilkaar.resultat, Utfall.IKKE_VURDERT) }

            vilkaar.filter { vilkarIkkeOppfylt.contains(it.hovedvilkaar.type) }
                .forEach { assertEquals(it.hovedvilkaar.resultat, Utfall.IKKE_OPPFYLT) }
        }
    }

    @Test
    fun `Skal sette yrkesskade som oppfylt ved migrering dersom avdoede hadde yrkesskade`() {
        testApplication {
            runServer(server) {
                vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
                migrering(migreringService, behandlingKlient, vilkaarsvurderingServiceImpl)
            }

            val response =
                client.post("/api/vilkaarsvurdering/migrering/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(VilkaarsvurderingMigreringRequest(yrkesskadeFordel = true).toJson())
                }
            assertEquals(HttpStatusCode.Accepted, response.status)

            val vilkaar =
                client.get("/api/vilkaarsvurdering/$behandlingId") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                    .also { assertTrue(it.status.isSuccess()) }
                    .let { objectMapper.readValue(it.bodyAsText(), VilkaarsvurderingDto::class.java) }
                    .vilkaar
                    .also { assertEquals(it.size, 8) }
            val vilkaarOppfylles =
                listOf(
                    VilkaarType.BP_FORMAAL_2024,
                    VilkaarType.BP_DOEDSFALL_FORELDER_2024,
                    VilkaarType.BP_YRKESSKADE_AVDOED_2024,
                    VilkaarType.BP_ALDER_BARN_2024,
                    VilkaarType.BP_FORTSATT_MEDLEMSKAP_2024,
                )
            vilkaar.filter { vilkaarOppfylles.contains(it.hovedvilkaar.type) }
                .forEach { assertEquals(it.hovedvilkaar.resultat, Utfall.OPPFYLT) }
            vilkaar.filter { !vilkaarOppfylles.contains(it.hovedvilkaar.type) }
                .forEach { assertEquals(it.hovedvilkaar.resultat, Utfall.IKKE_VURDERT) }
        }
    }
}
