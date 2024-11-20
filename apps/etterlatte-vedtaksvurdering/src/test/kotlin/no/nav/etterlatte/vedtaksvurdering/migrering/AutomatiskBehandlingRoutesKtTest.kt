package no.nav.etterlatte.vedtaksvurdering.migrering

import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.vedtaksvurdering.AutomatiskBehandlingService
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.SAKSBEHANDLER_1
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingService
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRapidService
import no.nav.etterlatte.vedtaksvurdering.automatiskBehandlingRoutes
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.vedtak
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AutomatiskBehandlingRoutesKtTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val vedtakService: VedtakBehandlingService = mockk()
    private val rapidService: VedtaksvurderingRapidService = mockk()
    private val automatiskBehandlingService = AutomatiskBehandlingService(vedtakService, behandlingKlient)

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
    }

    @BeforeEach
    fun beforeEach() {
        coEvery { behandlingKlient.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal opprette vedtak, fatte vedtak og attestere`() {
        testApplication {
            val opprettetVedtak = vedtak()
            val behandlingId = UUID.randomUUID()
            coEvery { vedtakService.opprettEllerOppdaterVedtak(any(), any()) } returns
                opprettetVedtak
            coEvery { vedtakService.fattVedtak(behandlingId, any(), any()) } returns
                VedtakOgRapid(
                    opprettetVedtak.toDto(),
                    RapidInfo(
                        VedtakKafkaHendelseHendelseType.FATTET,
                        opprettetVedtak.toDto(),
                        Tidspunkt.now(),
                        behandlingId,
                    ),
                )
            coEvery { vedtakService.hentVedtakForBehandling(any(), any()) } returns null
            coEvery { behandlingKlient.hentOppgaverForSak(any(), any()) } returns
                listOf(lagOppgave(behandlingId, Status.ATTESTERING))
            coEvery { behandlingKlient.tildelSaksbehandler(any(), any()) } returns true
            coEvery {
                vedtakService.attesterVedtak(
                    behandlingId,
                    any(),
                    any(),
                    any(),
                )
            } returns
                VedtakOgRapid(
                    opprettetVedtak.toDto(),
                    RapidInfo(
                        VedtakKafkaHendelseHendelseType.ATTESTERT,
                        opprettetVedtak.toDto(),
                        Tidspunkt.now(),
                        behandlingId,
                    ),
                )
            coEvery { rapidService.sendToRapid(any()) } just runs

            runServer(mockOAuth2Server) {
                automatiskBehandlingRoutes(
                    automatiskBehandlingService,
                    behandlingKlient,
                )
            }

            val respons =
                client
                    .post("/api/vedtak/1/$behandlingId/automatisk") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.let {
                        it.status shouldBe HttpStatusCode.OK
                        deserialize<VedtakOgRapid>(it.bodyAsText())
                    }

            assertEquals(respons.vedtak.id, opprettetVedtak.id)
            assertEquals(respons.rapidInfo1.vedtakhendelse, VedtakKafkaHendelseHendelseType.FATTET)
            assertEquals(respons.rapidInfo2!!.vedtakhendelse, VedtakKafkaHendelseHendelseType.ATTESTERT)
            coVerify(atMost = 1) {
                vedtakService.hentVedtakForBehandling(any(), any())
            }
            coVerify(exactly = 1) {
                vedtakService.opprettEllerOppdaterVedtak(behandlingId, any())
                behandlingKlient.hentOppgaverForSak(sakId1, any())
                vedtakService.fattVedtak(behandlingId, any(), Fagsaksystem.EY.navn)
                behandlingKlient.tildelSaksbehandler(any(), any())
                vedtakService.attesterVedtak(behandlingId, any(), any(), Fagsaksystem.EY.navn)
            }
            coVerify(atLeast = 1) {
                behandlingKlient.harTilgangTilBehandling(any(), any(), any())
            }
        }
    }

    @Nested
    @DisplayName("Skal stegvis opprette vedtak, fatte vedtak og attestere")
    inner class StegvisAutomatiskBehandling {
        @Test
        fun `Full kjoring skal skal opprette vedtak, fatte vedtak og attestere`() {
            testApplication {
                val opprettetVedtak = vedtak()
                val behandlingId = UUID.randomUUID()
                coEvery { vedtakService.hentVedtakForBehandling(any(), any()) } returns null
                coEvery { vedtakService.opprettEllerOppdaterVedtak(any(), any()) } returns
                    opprettetVedtak
                coEvery { vedtakService.fattVedtak(behandlingId, any(), any()) } returns
                    VedtakOgRapid(
                        opprettetVedtak.toDto(),
                        RapidInfo(
                            VedtakKafkaHendelseHendelseType.FATTET,
                            opprettetVedtak.toDto(),
                            Tidspunkt.now(),
                            behandlingId,
                        ),
                    )
                coEvery { behandlingKlient.hentOppgaverForSak(any(), any()) } returns
                    listOf(lagOppgave(behandlingId, Status.ATTESTERING))
                coEvery { behandlingKlient.tildelSaksbehandler(any(), any()) } returns true
                coEvery {
                    vedtakService.attesterVedtak(
                        behandlingId,
                        any(),
                        any(),
                        any(),
                    )
                } returns
                    VedtakOgRapid(
                        opprettetVedtak.toDto(),
                        RapidInfo(
                            VedtakKafkaHendelseHendelseType.ATTESTERT,
                            opprettetVedtak.toDto(),
                            Tidspunkt.now(),
                            behandlingId,
                        ),
                    )

                coEvery { rapidService.sendToRapid(any()) } just runs

                runServer(mockOAuth2Server) {
                    automatiskBehandlingRoutes(
                        automatiskBehandlingService,
                        behandlingKlient,
                    )
                }

                val respons =
                    client
                        .post("/api/vedtak/1/$behandlingId/automatisk/stegvis") {
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(HttpHeaders.Authorization, "Bearer $token")
                            setBody(MigreringKjoringVariant.FULL_KJORING.toJson())
                        }.let {
                            it.status shouldBe HttpStatusCode.OK
                            deserialize<VedtakOgRapid>(it.bodyAsText())
                        }

                assertEquals(respons.vedtak.id, opprettetVedtak.id)
                assertEquals(respons.rapidInfo1.vedtakhendelse, VedtakKafkaHendelseHendelseType.FATTET)
                assertEquals(respons.rapidInfo2!!.vedtakhendelse, VedtakKafkaHendelseHendelseType.ATTESTERT)
                coVerify(exactly = 1) {
                    vedtakService.hentVedtakForBehandling(behandlingId, any())
                    vedtakService.opprettEllerOppdaterVedtak(behandlingId, any())
                    behandlingKlient.hentOppgaverForSak(sakId1, any())
                    vedtakService.fattVedtak(behandlingId, any(), Fagsaksystem.EY.navn)
                    behandlingKlient.tildelSaksbehandler(any(), any())
                    vedtakService.attesterVedtak(behandlingId, any(), any(), Fagsaksystem.EY.navn)
                }
                coVerify(atLeast = 1) {
                    behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                }
            }
        }

        @Test
        fun `Med pause skal skal opprette vedtak og fatte vedtak`() {
            testApplication {
                val opprettetVedtak = vedtak()
                val behandlingId = UUID.randomUUID()
                coEvery { vedtakService.hentVedtakForBehandling(any(), any()) } returns null
                coEvery { runBlocking { vedtakService.opprettEllerOppdaterVedtak(any(), any()) } } returns
                    opprettetVedtak
                coEvery { runBlocking { vedtakService.fattVedtak(behandlingId, any(), any()) } } returns
                    VedtakOgRapid(
                        opprettetVedtak.toDto(),
                        RapidInfo(
                            VedtakKafkaHendelseHendelseType.FATTET,
                            opprettetVedtak.toDto(),
                            Tidspunkt.now(),
                            behandlingId,
                        ),
                    )
                coEvery { runBlocking { behandlingKlient.hentOppgaverForSak(any(), any()) } } returns
                    listOf(lagOppgave(behandlingId, Status.ATTESTERING))
                coEvery { runBlocking { behandlingKlient.tildelSaksbehandler(any(), any()) } } returns true

                runServer(mockOAuth2Server) {
                    automatiskBehandlingRoutes(
                        automatiskBehandlingService,
                        behandlingKlient,
                    )
                }

                val respons =
                    client
                        .post("/api/vedtak/1/$behandlingId/automatisk/stegvis") {
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(HttpHeaders.Authorization, "Bearer $token")
                            setBody(MigreringKjoringVariant.MED_PAUSE.toJson())
                        }.let {
                            it.status shouldBe HttpStatusCode.OK
                            deserialize<VedtakOgRapid>(it.bodyAsText())
                        }

                assertEquals(respons.vedtak.id, opprettetVedtak.id)
                coVerify(atMost = 1) {
                    vedtakService.hentVedtakForBehandling(any(), any())
                }
                coVerify(exactly = 1) {
                    vedtakService.opprettEllerOppdaterVedtak(behandlingId, any())
                    behandlingKlient.hentOppgaverForSak(sakId1, any())
                    vedtakService.fattVedtak(behandlingId, any(), Fagsaksystem.EY.navn)
                    behandlingKlient.tildelSaksbehandler(any(), any())
                }
                coVerify(atLeast = 1) {
                    behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                }
            }
        }

        @Test
        fun `Fortsett etter pause skal attestere vedtak`() {
            testApplication {
                val opprettetVedtak = vedtak()
                val behandlingId = UUID.randomUUID()
                coEvery {
                    runBlocking {
                        vedtakService.attesterVedtak(
                            behandlingId,
                            any(),
                            any(),
                            any(),
                        )
                    }
                } returns
                    VedtakOgRapid(
                        opprettetVedtak.toDto(),
                        RapidInfo(
                            VedtakKafkaHendelseHendelseType.ATTESTERT,
                            opprettetVedtak.toDto(),
                            Tidspunkt.now(),
                            behandlingId,
                        ),
                    )

                runServer(mockOAuth2Server) {
                    automatiskBehandlingRoutes(
                        automatiskBehandlingService,
                        behandlingKlient,
                    )
                }

                val respons =
                    client
                        .post("/api/vedtak/1/$behandlingId/automatisk/stegvis") {
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(HttpHeaders.Authorization, "Bearer $token")
                            setBody(MigreringKjoringVariant.FORTSETT_ETTER_PAUSE.toJson())
                        }.let {
                            it.status shouldBe HttpStatusCode.OK
                            deserialize<VedtakOgRapid>(it.bodyAsText())
                        }

                assertEquals(respons.vedtak.id, opprettetVedtak.id)
                coVerify(exactly = 1) {
                    vedtakService.attesterVedtak(behandlingId, any(), any(), Fagsaksystem.EY.navn)
                }
                coVerify(atLeast = 1) {
                    behandlingKlient.harTilgangTilBehandling(any(), any(), any())
                }
            }
        }
    }

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken(navIdent = SAKSBEHANDLER_1) }

    private fun lagOppgave(
        referanse: UUID,
        status: Status = Status.UNDER_BEHANDLING,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = status,
        enhet = Enheter.defaultEnhet.enhetNr,
        sakId = sakId1,
        kilde = null,
        type = OppgaveType.FOERSTEGANGSBEHANDLING,
        saksbehandler = null,
        referanse = referanse.toString(),
        merknad = null,
        sakType = SakType.BARNEPENSJON,
        fnr = null,
        frist = null,
        opprettet = Tidspunkt.now(),
    )
}
