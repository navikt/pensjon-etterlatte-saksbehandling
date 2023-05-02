package vilkaarsvurdering.migrering

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import no.nav.etterlatte.vilkaarsvurdering.migrering.MigreringRepository
import no.nav.etterlatte.vilkaarsvurdering.migrering.MigreringService
import no.nav.etterlatte.vilkaarsvurdering.migrering.migrering
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
import testsupport.buildTestApplicationConfigurationForOauth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private val server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig
    private val behandlingKlient = mockk<BehandlingKlient>()

    private lateinit var ds: DataSource
    private lateinit var migreringService: MigreringService
    private lateinit var vilkaarsvurderingRepository: VilkaarsvurderingRepository
    private lateinit var vilkaarsvurderingServiceImpl: VilkaarsvurderingService

    private val vilkaar: List<Vilkaar> = listOf(
        Vilkaar(
            hovedvilkaar = Delvilkaar(
                type = VilkaarType.BP_ALDER_BARN,
                tittel = "Alder barn",
                lovreferanse = Lovreferanse("1a")
            )
        )
    )

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(
            httpServer.port(),
            AZURE_ISSUER,
            CLIENT_ID
        )
        postgreSQLContainer.start()
        ds = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }

        val delvilkaarRepository = DelvilkaarRepository()
        vilkaarsvurderingRepository = VilkaarsvurderingRepository(ds, delvilkaarRepository)

        val grunnlagKlient = mockk<GrunnlagKlient>()
        vilkaarsvurderingServiceImpl =
            VilkaarsvurderingService(
                vilkaarsvurderingRepository,
                behandlingKlient,
                grunnlagKlient
            )

        migreringService = MigreringService(
            MigreringRepository(delvilkaarRepository, ds),
            vilkaarsvurderingRepository
        )
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
        coEvery { behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(any(), any()) } returns true
        coEvery { behandlingKlient.hentBehandling(behandlingId, any()) } returns detaljertBehandling()
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, any()) } returns true
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
    }

    private fun detaljertBehandling() = mockk<DetaljertBehandling>().apply {
        every { id } returns behandlingId
        every { sak } returns 1L
        every { sakType } returns SakType.BARNEPENSJON
        every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
        every { soeker } returns "10095512345"
        every { virkningstidspunkt } returns VirkningstidspunktTestData.virkningstidsunkt()
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

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        const val CLIENT_ID = "azure-id for saksbehandler"
    }

    @Test
    fun testMigrering() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application {
                restModule(this.log) {
                    vilkaarsvurdering(vilkaarsvurderingServiceImpl, behandlingKlient)
                    migrering(migreringService, behandlingKlient, vilkaarsvurderingServiceImpl)
                }
            }

            val response =
                client.post("/api/vilkaarsvurdering/migrering/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            assertEquals(HttpStatusCode.Accepted, response.status)

            client.get("/api/vilkaarsvurdering/$behandlingId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
                .also { assertTrue(it.status.isSuccess()) }
                .let { objectMapper.readValue(it.bodyAsText(), VilkaarsvurderingDto::class.java) }
                .vilkaar
                .also { assertEquals(it.size, 6) }
                .map { it.hovedvilkaar }
                .map { it.resultat }
                .forEach { assertTrue(it == Utfall.IKKE_VURDERT) }
        }
    }
}