package vilkaarsvurdering.migrering

import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.vilkaarsvurdering.DelvilkaarRepository
import no.nav.etterlatte.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepository
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.migrering.MigreringRepository
import no.nav.etterlatte.vilkaarsvurdering.migrering.MigreringService
import no.nav.etterlatte.vilkaarsvurdering.migrering.migrering
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import testsupport.buildTestApplicationConfigurationForOauth
import java.time.YearMonth
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

        val vilkaarsvurderingRepository = mockk<VilkaarsvurderingRepository>()
        every { vilkaarsvurderingRepository.hent(any()) } returns Vilkaarsvurdering(
            behandlingId = behandlingId,
            grunnlagVersjon = 1L,
            virkningstidspunkt = YearMonth.now(),
            vilkaar = vilkaar
        )
        migreringService = MigreringService(
            MigreringRepository(DelvilkaarRepository(), ds),
            vilkaarsvurderingRepository
        )
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
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
        val oboToken = Bruker.of("token", "s1", null, null, null)
        const val CLIENT_ID = "azure-id for saksbehandler"
    }

    @Test
    fun testMigrering() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            application { restModule(this.log) { migrering(migreringService, behandlingKlient) } }

            val response =
                client.patch("/api/vilkaarsvurdering2/migrering/$behandlingId/vilkaar/utfall/${Utfall.IKKE_VURDERT}") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            Assertions.assertEquals(HttpStatusCode.Accepted, response.status)
        }
    }
}