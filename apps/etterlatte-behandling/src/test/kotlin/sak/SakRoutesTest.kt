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
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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
    private val tilgangService = mockk<TilgangService>(relaxUnitFun = true)
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
    fun `Returnerer ok ved endring av enhet med EnhetsRequest`() {
        coEvery {
            sakService.oppdaterEnhetForSaker(any())
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
        } just runs
        every { sakService.finnSak(any()) } returns
            Sak(
                ident = "12345",
                sakType = SakType.BARNEPENSJON,
                id = randomSakId(),
                enhet = Enheter.defaultEnhet.enhetNr,
            )
        every { oppgaveService.hentOppgaverForSak(any()) } returns
            listOf(
                OppgaveIntern(
                    id = UUID.randomUUID(),
                    status = Status.UNDER_BEHANDLING,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                    sakId = sakId1,
                    kilde = null,
                    type = OppgaveType.FOERSTEGANGSBEHANDLING,
                    saksbehandler = OppgaveSaksbehandler("Rask Spaghetti"),
                    referanse = "hmm",
                    merknad = null,
                    opprettet = Tidspunkt.now(),
                    sakType = SakType.BARNEPENSJON,
                    fnr = "123",
                    frist = null,
                ),
                OppgaveIntern(
                    id = UUID.randomUUID(),
                    status = Status.UNDER_BEHANDLING,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                    sakId = sakId1,
                    kilde = null,
                    type = OppgaveType.KLAGE,
                    saksbehandler = null,
                    referanse = "hmm",
                    merknad = null,
                    opprettet = Tidspunkt.now(),
                    sakType = SakType.BARNEPENSJON,
                    fnr = "123",
                    frist = null,
                ),
                OppgaveIntern(
                    id = UUID.randomUUID(),
                    status = Status.FERDIGSTILT,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                    sakId = sakId1,
                    kilde = null,
                    type = OppgaveType.KLAGE,
                    saksbehandler = OppgaveSaksbehandler("Rask Spaghetti"),
                    referanse = "hmm",
                    merknad = null,
                    opprettet = Tidspunkt.now(),
                    sakType = SakType.BARNEPENSJON,
                    fnr = "123",
                    frist = null,
                ),
            )
        withTestApplication { client ->
            val response =
                client.post("/api/sak/1/endre_enhet") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(EnhetRequest(enhet = Enheter.PORSGRUNN.enhetNr))
                }
            assertEquals(200, response.status.value)
            verify(exactly = 1) { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) }
            verify(exactly = 1) { sakService.oppdaterEnhetForSaker(any()) }
        }
    }

    @Test
    fun `Returnerer badrequest ved endring av enhet med ugyldig enhet`() {
        coEvery {
            sakService.oppdaterEnhetForSaker(any())
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
        } just runs
        every { sakService.finnSak(any()) } returns null
        every { oppgaveService.hentOppgaverForSak(any()) } returns emptyList()

        withTestApplication { client ->
            val response =
                client.post("/api/sak/1/endre_enhet") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(EnhetRequest(enhet = Enhetsnummer("4805")))
                }
            assertEquals(400, response.status.value)
            verify(exactly = 0) { sakService.finnSak(any()) }
            verify(exactly = 0) { oppgaveService.hentOppgaverForSak(any()) }
            verify(exactly = 0) { oppgaveService.fjernSaksbehandler(any()) }
        }
    }

    @Test
    fun `Returnerer bad request hvis sak ikke finnes ved endring av enhet`() {
        coEvery {
            sakService.oppdaterEnhetForSaker(any())
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
        } just runs
        every { sakService.finnSak(any()) } returns null
        withTestApplication { client ->
            val response =
                client.post("/api/sak/1/endre_enhet") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(EnhetRequest(enhet = Enheter.PORSGRUNN.enhetNr))
                }
            assertEquals(400, response.status.value)
        }
    }

    @Test
    fun `siste iverksatte route returnerer 200 ok og behandling`() {
        coEvery { behandlingService.hentSisteIverksatte(sakId1) } returns mockk(relaxed = true)

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
        coEvery { behandlingService.hentSisteIverksatte(sakId1) } returns null
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
            )

        every { sakService.finnSak(any()) } returns sak
        every { sakService.oppdaterIdentForSak(any()) } returns sak.copy(ident = nyIdent.value)
        val behandlingOgSak = BehandlingOgSak(UUID.randomUUID(), sakId)
        every { behandlingService.hentAapneBehandlingerForSak(sakId) } returns listOf(behandlingOgSak)

        withTestApplication { client ->
            val response =
                client.post("/api/sak/$sakId/oppdater_ident?hendelseId=$hendelseId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                }
            assertEquals(200, response.status.value)
        }

        verify(exactly = 1) {
            sakService.finnSak(sakId)
            sakService.oppdaterIdentForSak(sak)
            behandlingService.hentAapneBehandlingerForSak(sakId)
            behandlingService.avbrytBehandling(behandlingOgSak.behandlingId, any(), AarsakTilAvbrytelse.ANNET, any())
            grunnlagsendringshendelseService.arkiverHendelseMedKommentar(hendelseId, any(), any())
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
