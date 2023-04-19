package no.nav.etterlatte

import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingServiceImpl
import no.nav.etterlatte.vilkaarsvurdering.vilkaarsvurdering
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import testsupport.buildTestApplicationConfigurationForOauth
import verifiserAtRutaMatcherDetKlientenKallerForPost
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarsvurderingAPITest {

    private val clientID = "azure-id for saksbehandler"

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private val server = MockOAuth2Server()
    private lateinit var hoconApplicationConfig: HoconApplicationConfig

    private lateinit var ds: DataSource

    @BeforeAll
    fun before() {
        server.start()
        val httpServer = server.config.httpServer
        hoconApplicationConfig = buildTestApplicationConfigurationForOauth(httpServer.port(), AZURE_ISSUER, clientID)
        postgreSQLContainer.start()
        ds = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }
    }

    @AfterAll
    fun after() {
        server.shutdown()
        postgreSQLContainer.stop()
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = clientID,
            claims = mapOf(
                "navn" to "John Doe",
                "NAVident" to "Saksbehandler01"
            )
        ).serialize()
    }

    @Test
    fun sign() {
        testApplication {
            environment {
                config = hoconApplicationConfig
            }
            val (sti, request) = kallKlient()
            klargjoerRute()
            verifiserAtRutaMatcherDetKlientenKallerForPost(sti, request, token)
        }
    }

    private fun ApplicationTestBuilder.klargjoerRute() {
        val vilkaarsvurderingService = mockk<VilkaarsvurderingService>()
            .also {
                coEvery { it.oppdaterTotalVurdering(any(), any(), any()) } returns
                    Vilkaarsvurdering(
                        behandlingId = UUID.randomUUID(),
                        grunnlagVersjon = 1,
                        virkningstidspunkt = YearMonth.now(),
                        vilkaar = listOf()
                    )
            }

        val behandlingKlient = mockk<BehandlingKlient>().also {
            coEvery { it.harTilgangTilBehandling(any(), any()) } returns true
        }
        application {
            restModule(this.log) {
                vilkaarsvurdering(
                    vilkaarsvurderingService,
                    behandlingKlient
                )
            }
        }
    }

    private fun kallKlient(): Pair<String, VurdertVilkaarsvurderingResultatDto> {
        val sti = slot<String>()
        val request = VurdertVilkaarsvurderingResultatDto(VilkaarsvurderingUtfall.OPPFYLT, "")
        val klient = spyk(VilkaarsvurderingServiceImpl(mockk(), ""))
        coEvery { klient.post(any(), capture(sti)) } returns mockk()
        klient.oppdaterTotalVurdering(UUID.randomUUID(), request)
        return Pair(sti.captured, request)
    }
}