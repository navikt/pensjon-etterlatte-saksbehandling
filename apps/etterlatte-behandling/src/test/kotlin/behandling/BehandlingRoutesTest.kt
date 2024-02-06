package no.nav.etterlatte.behandling

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.ktor.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.sak.UtlandstilknytningRequest
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingService = mockk<BehandlingService>(relaxUnitFun = true)
    private val gyldighetsproevingService = mockk<GyldighetsproevingService>()
    private val kommerBarnetTilGodeService = mockk<KommerBarnetTilGodeService>()
    private val aktivitetspliktService = mockk<AktivitetspliktService>()
    private val behandlingFactory = mockk<BehandlingFactory>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.start()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `kan oppdater bodd eller arbeidet i utlandet`() {
        coEvery {
            behandlingService.oppdaterBoddEllerArbeidetUtlandet(any(), any())
        } just runs

        withTestApplication { client ->
            val response =
                client.post("/api/behandling/$behandlingId/boddellerarbeidetutlandet") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(BoddEllerArbeidetUtlandetRequest(true, "Test"))
                }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `kan lagre virkningstidspunkt hvis det er gyldig`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"

        mockBehandlingService(bodyVirkningstidspunkt, bodyBegrunnelse)

        coEvery {
            behandlingService.erGyldigVirkningstidspunkt(any(), any(), any())
        } returns true

        withTestApplication { client ->
            val response =
                client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                        "dato":"$bodyVirkningstidspunkt",
                        "begrunnelse":"$bodyBegrunnelse"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `Avbryt behandling`() {
        withTestApplication { client ->
            val response =
                client.post("/api/behandling/$behandlingId/avbryt") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(200, response.status.value)
            verify(exactly = 1) { behandlingService.avbrytBehandling(behandlingId, any()) }
        }
    }

    @Test
    fun `Gir bad request hvis virkningstidspunkt ikke er gyldig`() {
        val bodyVirkningstidspunkt = Tidspunkt.parse("2017-02-01T00:00:00Z")
        val bodyBegrunnelse = "begrunnelse"

        mockBehandlingService(bodyVirkningstidspunkt, bodyBegrunnelse)

        coEvery {
            behandlingService.erGyldigVirkningstidspunkt(any(), any(), any())
        } returns false

        withTestApplication { client ->
            val response =
                client.post("/api/behandling/$behandlingId/virkningstidspunkt") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                        "dato":"$bodyVirkningstidspunkt",
                        "begrunnelse":"$bodyBegrunnelse"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(400, response.status.value)
        }
    }

    @Test
    fun `kan oppdatere utlandstilknytning`() {
        coEvery {
            behandlingService.oppdaterUtlandstilknytning(any(), any())
        } just runs

        withTestApplication { client ->
            val response =
                client.post("/api/behandling/${UUID.randomUUID()}/utlandstilknytning") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(UtlandstilknytningRequest(UtlandstilknytningType.BOSATT_UTLAND, "Test"))
                }

            assertEquals(200, response.status.value)
        }
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        val user = mockk<SaksbehandlerMedEnheterOgRoller>()
        every { user.harSkrivetilgang() } returns true
        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext(user)
                    behandlingRoutes(
                        behandlingService,
                        gyldighetsproevingService,
                        kommerBarnetTilGodeService,
                        aktivitetspliktService,
                        behandlingFactory,
                    )
                }
            block(client)
        }
    }

    private fun mockBehandlingService(
        bodyVirkningstidspunkt: Tidspunkt,
        bodyBegrunnelse: String,
    ) {
        val parsetVirkningstidspunkt =
            YearMonth.from(
                bodyVirkningstidspunkt.toNorskTid().let {
                    YearMonth.of(it.year, it.month)
                },
            )
        val virkningstidspunkt =
            Virkningstidspunkt(
                parsetVirkningstidspunkt,
                Grunnlagsopplysning.Saksbehandler.create(NAV_IDENT),
                bodyBegrunnelse,
            )
        every {
            behandlingService.oppdaterVirkningstidspunkt(
                behandlingId,
                parsetVirkningstidspunkt,
                NAV_IDENT,
                bodyBegrunnelse,
            )
        } returns virkningstidspunkt
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken(navIdent = NAV_IDENT) }

    private companion object {
        val behandlingId: UUID = UUID.randomUUID()
        const val NAV_IDENT = "Saksbehandler01"
    }
}
