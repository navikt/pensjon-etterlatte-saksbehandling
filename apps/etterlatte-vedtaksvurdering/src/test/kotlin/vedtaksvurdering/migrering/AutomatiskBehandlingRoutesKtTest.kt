package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveListe
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import testsupport.buildTestApplicationConfigurationForOauth
import vedtaksvurdering.SAKSBEHANDLER_1
import vedtaksvurdering.vedtak
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AutomatiskBehandlingRoutesKtTest {

    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val vedtaksvurderingService: VedtaksvurderingService = mockk()

    @BeforeAll
    fun before() {
        server.start()

        applicationConfig =
            buildTestApplicationConfigurationForOauth(server.config.httpServer.port(), AZURE_ISSUER, CLIENT_ID)
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
    }

    @BeforeEach
    fun beforeEach() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any()) } returns true
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun `skal opprette vedtak, fatte vedtak og attestere`() {
        testApplication {
            val opprettetVedtak = vedtak()
            val behandlingId = UUID.randomUUID()
            every { runBlocking { vedtaksvurderingService.opprettEllerOppdaterVedtak(any(), any()) } } returns
                opprettetVedtak
            every { runBlocking { vedtaksvurderingService.fattVedtak(behandlingId, any()) } } returns opprettetVedtak
            every { runBlocking { behandlingKlient.hentOppgaverForSak(any(), any()) } } returns OppgaveListe(
                mockk(),
                listOf(lagOppgave(behandlingId))
            )
            every { runBlocking { behandlingKlient.tildelSaksbehandler(any(), any()) } } returns true
            every {
                runBlocking {
                    vedtaksvurderingService.attesterVedtak(
                        behandlingId,
                        any(),
                        any()
                    )
                }
            } returns opprettetVedtak

            environment { config = applicationConfig }
            application { restModule(log) { automatiskBehandlingRoutes(vedtaksvurderingService, behandlingKlient) } }

            val vedtak = client.post("/api/vedtak/1/$behandlingId/automatisk") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }.let {
                it.status shouldBe HttpStatusCode.OK
                deserialize<VedtakDto>(it.bodyAsText())
            }

            Assertions.assertEquals(vedtak.vedtakId, opprettetVedtak.id)

            coVerify(exactly = 1) {
                vedtaksvurderingService.opprettEllerOppdaterVedtak(behandlingId, any())
                behandlingKlient.hentOppgaverForSak(1, any())
                vedtaksvurderingService.fattVedtak(behandlingId, any())
                behandlingKlient.tildelSaksbehandler(any(), any())
                vedtaksvurderingService.attesterVedtak(behandlingId, any(), any())
            }
            coVerify(atLeast = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any())
            }
        }
    }

    private val token: String by lazy {
        server.issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims = mapOf("navn" to "John Doe", "NAVident" to SAKSBEHANDLER_1)
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
    }

    fun lagOppgave(referanse: UUID) = OppgaveNy(
        id = UUID.randomUUID(),
        status = Status.UNDER_BEHANDLING,
        enhet = "",
        sakId = 1,
        kilde = null,
        type = OppgaveType.ATTESTERING,
        saksbehandler = null,
        referanse = referanse.toString(),
        merknad = null,
        sakType = SakType.BARNEPENSJON,
        fnr = null,
        frist = null,
        opprettet = Tidspunkt.now()
    )
}