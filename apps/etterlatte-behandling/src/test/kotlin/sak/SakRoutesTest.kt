package no.nav.etterlatte.sak

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingService = mockk<BehandlingService>(relaxUnitFun = true)
    private val sakService = mockk<SakService>(relaxUnitFun = true)
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>(relaxUnitFun = true)
    private val tilgangService = mockk<TilgangServiceSjekker>(relaxUnitFun = true)
    private val oppgaveService = mockk<OppgaveService>(relaxUnitFun = true)
    private val requestLogger = mockk<BehandlingRequestLogger>()
    private val hendelseDao = mockk<HendelseDao>()

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
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
    fun `siste iverksatte route returnerer 200 ok og behandling`() {
        coEvery { behandlingService.hentSisteIverksatteBehandling(sakId1) } returns mockk(relaxed = true)

        withTestApplication { client ->
            val response =
                client.get("/saker/1/behandlinger/sisteIverksatte") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(200, response.status.value)
        }
    }

    @Test
    fun `siste iverksatte route returnerer 404 naar det ikke finnes noen iverksatt behandling`() {
        coEvery { behandlingService.hentSisteIverksatteBehandling(sakId1) } returns null
        withTestApplication { client ->
            val response =
                client.get("/saker/1/behandlinger/sisteIverksatte") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            assertEquals(404, response.status.value)
        }
    }

    @Test
    fun `Oppdater ident på sak`() {
        val nyIdent = KONTANT_FOT
        val sakId = SakId(Random.nextLong())
        val hendelseId = UUID.randomUUID()
        val sak =
            Sak(
                "ident",
                SakType.OMSTILLINGSSTOENAD,
                sakId,
                Enheter.defaultEnhet.enhetNr,
                null,
                false,
            )

        every { sakService.finnSak(any()) } returns sak
        every { sakService.oppdaterIdentForSak(any(), any()) } returns sak.copy(ident = nyIdent.value)
        val behandlingOgSak = BehandlingOgSak(UUID.randomUUID(), sakId)
        every { behandlingService.hentAapneBehandlingerForSak(sakId) } returns listOf(behandlingOgSak)

        withTestApplication { client ->
            val response =
                client.post("/api/sak/$sakId/oppdater-ident") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(OppdaterIdentRequest(hendelseId))
                }
            assertEquals(200, response.status.value)
        }

        verify(exactly = 1) {
            sakService.finnSak(sakId)
            sakService.oppdaterIdentForSak(sak, any())
            behandlingService.hentAapneBehandlingerForSak(sakId)
            behandlingService.avbrytBehandling(
                behandlingOgSak.behandlingId,
                any(),
                AarsakTilAvbrytelse.ENDRET_FOLKEREGISTERIDENT,
                any(),
            )
            grunnlagsendringshendelseService.arkiverHendelseMedKommentar(hendelseId, any(), any())
        }
    }

    @Test
    fun `Oppdater ident på sak - uten hendelse`() {
        val nyIdent = KONTANT_FOT
        val sakId = SakId(Random.nextLong())
        val sak =
            Sak(
                "ident",
                SakType.OMSTILLINGSSTOENAD,
                sakId,
                Enheter.defaultEnhet.enhetNr,
                null,
                false,
            )

        every { sakService.finnSak(any()) } returns sak
        every { sakService.oppdaterIdentForSak(any(), any()) } returns sak.copy(ident = nyIdent.value)
        val behandlingOgSak = BehandlingOgSak(UUID.randomUUID(), sakId)
        every { behandlingService.hentAapneBehandlingerForSak(sakId) } returns listOf(behandlingOgSak)

        withTestApplication { client ->
            val response =
                client.post("/api/sak/$sakId/oppdater-ident") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(OppdaterIdentRequest(null, utenHendelse = true))
                }
            assertEquals(200, response.status.value)
        }

        verify(exactly = 1) {
            sakService.finnSak(sakId)
            sakService.oppdaterIdentForSak(sak, any())
            behandlingService.hentAapneBehandlingerForSak(sakId)
            behandlingService.avbrytBehandling(
                behandlingOgSak.behandlingId,
                any(),
                AarsakTilAvbrytelse.ENDRET_FOLKEREGISTERIDENT,
                any(),
            )
        }
        verify {
            grunnlagsendringshendelseService wasNot Called
        }
    }

    private fun withTestApplication(block: suspend (client: HttpClient) -> Unit) {
        val user =
            mockk<SaksbehandlerMedEnheterOgRoller>().also { every { it.name() } returns this::class.java.simpleName }

        every { user.enheterMedSkrivetilgang() } returns listOf(Enheter.defaultEnhet.enhetNr)

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext(user)
                    sakSystemRoutes(
                        tilgangService,
                        sakService,
                        behandlingService,
                        requestLogger,
                    )
                    sakWebRoutes(
                        tilgangService,
                        sakService,
                        behandlingService,
                        grunnlagsendringshendelseService,
                        oppgaveService,
                        requestLogger,
                        hendelseDao,
                    )
                }
            block(client)
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }
}
