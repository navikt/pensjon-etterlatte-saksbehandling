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
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
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
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AutomatiskBehandlingRoutesKtTest {
    private val server = MockOAuth2Server()
    private lateinit var applicationConfig: HoconApplicationConfig
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val service: VedtakBehandlingService = mockk()
    private val rapidService: VedtaksvurderingRapidService = mockk()

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
            coEvery { service.opprettEllerOppdaterVedtak(any(), any()) } returns
                opprettetVedtak
            coEvery { service.fattVedtak(behandlingId, any()) } returns
                VedtakOgRapid(
                    opprettetVedtak.toDto(),
                    mockk(),
                )
            coEvery { behandlingKlient.hentOppgaverForSak(any(), any()) } returns
                OppgaveListe(
                    mockk(),
                    listOf(lagOppgave(behandlingId)),
                )
            coEvery { behandlingKlient.tildelSaksbehandler(any(), any()) } returns true
            coEvery {
                service.attesterVedtak(
                    behandlingId,
                    any(),
                    any(),
                )
            } returns
                VedtakOgRapid(
                    opprettetVedtak.toDto(),
                    RapidInfo(VedtakKafkaHendelseType.ATTESTERT, opprettetVedtak.toNyDto(), Tidspunkt.now(), behandlingId),
                )
            coEvery { rapidService.sendToRapid(any()) } just runs

            environment { config = applicationConfig }
            application {
                restModule(log) {
                    automatiskBehandlingRoutes(
                        service,
                        rapidService,
                        behandlingKlient,
                    )
                }
            }

            val vedtak =
                client.post("/api/vedtak/1/$behandlingId/automatisk") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let {
                    it.status shouldBe HttpStatusCode.OK
                    deserialize<VedtakOgRapid>(it.bodyAsText())
                }

            Assertions.assertEquals(vedtak.vedtak.vedtakId, opprettetVedtak.id)
            Assertions.assertEquals(vedtak.rapidInfo1.vedtakhendelse, VedtakKafkaHendelseType.ATTESTERT)

            coVerify(exactly = 1) {
                service.opprettEllerOppdaterVedtak(behandlingId, any())
                behandlingKlient.hentOppgaverForSak(1, any())
                service.fattVedtak(behandlingId, any())
                behandlingKlient.tildelSaksbehandler(any(), any())
                service.attesterVedtak(behandlingId, any(), any())
            }
            coVerify(exactly = 2) {
                rapidService.sendToRapid(any())
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
            claims = mapOf("navn" to "John Doe", "NAVident" to SAKSBEHANDLER_1),
        ).serialize()
    }

    private companion object {
        const val CLIENT_ID = "azure-id for saksbehandler"
    }

    private fun lagOppgave(referanse: UUID) =
        OppgaveIntern(
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
            opprettet = Tidspunkt.now(),
        )
}
