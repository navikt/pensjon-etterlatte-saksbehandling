package no.nav.etterlatte.behandling.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.asContextElement
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.vedtaksvurdering.routes.automatiskBehandlingRoutes
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.migrering.MigreringKjoringVariant
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
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
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class AutomatiskBehandlingRoutesTest(
    val dataSource: DataSource,
) {
    private val mockOAuth2Server = MockOAuth2Server()
    private val vedtakBehandlingService: VedtakBehandlingService = mockk()
    private val rapidService: VedtaksvurderingRapidService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val automatiskBehandlingService = AutomatiskBehandlingService(vedtakBehandlingService, oppgaveService)

    private val user: SaksbehandlerMedEnheterOgRoller = mockk()
    private lateinit var context: Context

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @BeforeEach
    fun beforeEach() {
        every { user.enheterMedSkrivetilgang() } returns listOf(Enheter.defaultEnhet.enhetNr)
        context =
            nyKontekstMedBrukerOgDatabase(
                user,
                dataSource,
            )
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(vedtakBehandlingService, rapidService, oppgaveService)
        clearAllMocks()
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal opprette vedtak, fatte vedtak og attestere`() {
        withTestApplication(context) { client ->
            val opprettetVedtak = vedtak()
            val behandlingId = UUID.randomUUID()

            coEvery { vedtakBehandlingService.opprettEllerOppdaterVedtak(any(), any()) } returns
                opprettetVedtak
            coEvery { vedtakBehandlingService.fattVedtak(behandlingId, any(), any()) } returns
                VedtakOgRapid(
                    vedtak = opprettetVedtak.toDto(),
                    rapidInfo1 =
                        RapidInfo(
                            vedtakhendelse = VedtakKafkaHendelseHendelseType.FATTET,
                            vedtak = opprettetVedtak.toDto(),
                            tekniskTid = Tidspunkt.now(),
                            behandlingId = behandlingId,
                        ),
                )
            coEvery { vedtakBehandlingService.hentVedtakForBehandling(any()) } returns null
            coEvery { oppgaveService.hentOppgaverForSak(any<SakId>()) } returns
                listOf(
                    lagOppgave(
                        referanse = behandlingId,
                        status = Status.ATTESTERING,
                    ),
                )
            coEvery { oppgaveService.tildelSaksbehandler(any(), any()) } just Runs
            coEvery {
                vedtakBehandlingService.attesterVedtak(
                    behandlingId = behandlingId,
                    kommentar = any(),
                    brukerTokenInfo = any(),
                    attestant = any(),
                )
            } returns
                VedtakOgRapid(
                    opprettetVedtak.toDto(),
                    RapidInfo(
                        vedtakhendelse = VedtakKafkaHendelseHendelseType.ATTESTERT,
                        vedtak = opprettetVedtak.toDto(),
                        tekniskTid = Tidspunkt.now(),
                        behandlingId = behandlingId,
                    ),
                )
            coEvery { rapidService.sendToRapid(any()) } just runs

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
                vedtakBehandlingService.hentVedtakForBehandling(any())
            }
            coVerify(exactly = 1) {
                vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId = behandlingId, brukerTokenInfo = any())
                oppgaveService.hentOppgaverForSak(sakId = sakId1)
                vedtakBehandlingService.fattVedtak(behandlingId, any(), Fagsaksystem.EY.navn)
                oppgaveService.tildelSaksbehandler(any(), any())
                vedtakBehandlingService.attesterVedtak(behandlingId, any(), any(), Fagsaksystem.EY.navn)
            }
        }
    }

    @Nested
    @DisplayName("Skal stegvis opprette vedtak, fatte vedtak og attestere")
    inner class StegvisAutomatiskBehandling {
        @Test
        fun `Full kjoring skal skal opprette vedtak, fatte vedtak og attestere`() {
            withTestApplication(context) { client ->
                val opprettetVedtak = vedtak()
                val behandlingId = UUID.randomUUID()
                coEvery { vedtakBehandlingService.hentVedtakForBehandling(any()) } returns null
                coEvery { vedtakBehandlingService.opprettEllerOppdaterVedtak(any(), any()) } returns
                    opprettetVedtak
                coEvery { vedtakBehandlingService.fattVedtak(behandlingId, any(), any()) } returns
                    VedtakOgRapid(
                        vedtak = opprettetVedtak.toDto(),
                        rapidInfo1 =
                            RapidInfo(
                                vedtakhendelse = VedtakKafkaHendelseHendelseType.FATTET,
                                vedtak = opprettetVedtak.toDto(),
                                tekniskTid = Tidspunkt.now(),
                                behandlingId = behandlingId,
                            ),
                    )
                coEvery { oppgaveService.hentOppgaverForSak(any<SakId>()) } returns
                    listOf(
                        lagOppgave(
                            referanse = behandlingId,
                            status = Status.ATTESTERING,
                        ),
                    )
                coEvery { oppgaveService.tildelSaksbehandler(any(), any()) } just runs
                coEvery {
                    vedtakBehandlingService.attesterVedtak(
                        behandlingId = behandlingId,
                        kommentar = any(),
                        brukerTokenInfo = any(),
                        attestant = any(),
                    )
                } returns
                    VedtakOgRapid(
                        vedtak = opprettetVedtak.toDto(),
                        rapidInfo1 =
                            RapidInfo(
                                vedtakhendelse = VedtakKafkaHendelseHendelseType.ATTESTERT,
                                vedtak = opprettetVedtak.toDto(),
                                tekniskTid = Tidspunkt.now(),
                                behandlingId = behandlingId,
                            ),
                    )

                coEvery { rapidService.sendToRapid(any()) } just runs

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
                    vedtakBehandlingService.hentVedtakForBehandling(behandlingId)
                    vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, any())
                    oppgaveService.hentOppgaverForSak(sakId1)
                    vedtakBehandlingService.fattVedtak(
                        behandlingId = behandlingId,
                        brukerTokenInfo = any(),
                        saksbehandler = Fagsaksystem.EY.navn,
                    )
                    oppgaveService.tildelSaksbehandler(any(), any())
                    vedtakBehandlingService.attesterVedtak(behandlingId, any(), any(), Fagsaksystem.EY.navn)
                }
            }
        }

        @Test
        fun `Med pause skal skal opprette vedtak og fatte vedtak`() {
            withTestApplication(context) { client ->
                val opprettetVedtak = vedtak()
                val behandlingId = UUID.randomUUID()
                coEvery { vedtakBehandlingService.hentVedtakForBehandling(any()) } returns null
                coEvery { vedtakBehandlingService.opprettEllerOppdaterVedtak(any(), any()) } returns
                    opprettetVedtak
                coEvery { vedtakBehandlingService.fattVedtak(behandlingId, any(), any()) } returns
                    VedtakOgRapid(
                        vedtak = opprettetVedtak.toDto(),
                        rapidInfo1 =
                            RapidInfo(
                                vedtakhendelse = VedtakKafkaHendelseHendelseType.FATTET,
                                vedtak = opprettetVedtak.toDto(),
                                tekniskTid = Tidspunkt.now(),
                                behandlingId = behandlingId,
                            ),
                    )
                coEvery { oppgaveService.hentOppgaverForSak(any<SakId>()) } returns
                    listOf(
                        lagOppgave(
                            behandlingId,
                            Status.ATTESTERING,
                        ),
                    )
                coEvery { oppgaveService.tildelSaksbehandler(any(), any()) } just Runs

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
                    vedtakBehandlingService.hentVedtakForBehandling(any())
                }
                coVerify(exactly = 1) {
                    vedtakBehandlingService.opprettEllerOppdaterVedtak(behandlingId, any())
                    oppgaveService.hentOppgaverForSak(sakId = sakId1)
                    vedtakBehandlingService.fattVedtak(
                        behandlingId = behandlingId,
                        brukerTokenInfo = any(),
                        saksbehandler = Fagsaksystem.EY.navn,
                    )
                    oppgaveService.tildelSaksbehandler(any(), any())
                }
            }
        }

        @Test
        fun `Fortsett etter pause skal attestere vedtak`() {
            withTestApplication(context) { client ->
                val opprettetVedtak = vedtak()
                val behandlingId = UUID.randomUUID()
                coEvery {
                    vedtakBehandlingService.attesterVedtak(
                        behandlingId = behandlingId,
                        kommentar = any(),
                        brukerTokenInfo = any(),
                        attestant = any(),
                    )
                } returns
                    VedtakOgRapid(
                        vedtak = opprettetVedtak.toDto(),
                        rapidInfo1 =
                            RapidInfo(
                                vedtakhendelse = VedtakKafkaHendelseHendelseType.ATTESTERT,
                                vedtak = opprettetVedtak.toDto(),
                                tekniskTid = Tidspunkt.now(),
                                behandlingId = behandlingId,
                            ),
                    )

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
                    vedtakBehandlingService.attesterVedtak(behandlingId, any(), any(), Fagsaksystem.EY.navn)
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
        gruppeId = null,
        merknad = null,
        sakType = SakType.BARNEPENSJON,
        fnr = null,
        frist = null,
        opprettet = Tidspunkt.now(),
    )

    private fun withTestApplication(
        context: Context,
        block: suspend (client: HttpClient) -> Unit,
    ) {
        testApplication(Kontekst.asContextElement(context)) {
            runServer(mockOAuth2Server) {
                automatiskBehandlingRoutes(
                    automatiskBehandlingService = automatiskBehandlingService,
                )
            }
            block(client)
        }
    }
}
