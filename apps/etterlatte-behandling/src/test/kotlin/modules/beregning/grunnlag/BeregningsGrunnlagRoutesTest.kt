package no.nav.etterlatte.modules.beregning.grunnlag

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling.BehandlingStoetterIkkeStatusEndringException
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.behandling.vedtaksvurdering.vedtak
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.LagreBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDTO
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagData
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.modules.beregning.BeregningRepository
import no.nav.etterlatte.modules.beregning.BeregningTestKontekst
import no.nav.etterlatte.modules.beregning.OverstyrBeregning
import no.nav.etterlatte.modules.beregning.OverstyrBeregningStatus
import no.nav.etterlatte.modules.beregning.regler.toGrunnlag
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BeregningTestKontekst::class)
internal class BeregningsGrunnlagRoutesTest {
    private val behandlingStatusService = mockk<BehandlingStatusService>()
    private val behandlingTilgangsSjekk = mockk<BehandlingTilgangsSjekk>()
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtaksvurderingService = mockk<VedtaksvurderingService>()
    private val repository = mockk<BeregningsGrunnlagRepository>()
    private val beregningRepository = mockk<BeregningRepository>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val sanksjonService = mockk<SanksjonService>()
    private val service =
        BeregningsGrunnlagService(
            repository,
            beregningRepository,
            behandlingService,
            vedtaksvurderingService,
            grunnlagService,
            sanksjonService,
            behandlingStatusService = behandlingStatusService,
            behandlingTilgangsSjekk = behandlingTilgangsSjekk,
        )

    @BeforeAll
    fun before() {
        mockOAuth2Server.startRandomPort()
    }

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()

        // Antar at alle behandlinger finnes, og er skrivbare og ikke-overstyrte
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns
            mockk {
                every { sak } returns SakId(123L)
            }
        coEvery { beregningRepository.hentOverstyrBeregning(any()) } returns null
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), true) } returns Unit
    }

    @AfterAll
    fun after() {
        mockOAuth2Server.shutdown()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun `skal returnere konflikt for statusfeil i beregningsgrunnlag uten aa lagre`(feiltype: Int) {
        val behandlingId = randomUUID()
        val feil =
            when (feiltype) {
                0 -> TilstandException.UgyldigTilstand("Ugyldig status")
                1 -> TilstandException.IkkeFyltUt("Behandlingen er ikke fylt ut")
                2 -> TilstandException.KanIkkeRedigere("Behandlingen kan ikke redigeres")
                else -> BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.TRYGDETID_OPPDATERT)
            }
        every { behandlingStatusService.settTrygdetidOppdatert(behandlingId, any(), true) } throws feil
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            val response =
                client.post("/api/beregning/beregningsgrunnlag/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LagreBeregningsGrunnlag().toJson())
                }

            response.status shouldBe HttpStatusCode.Conflict
            objectMapper.readTree(response.bodyAsText())["code"].asString() shouldBe "UNKNOWN_ERROR"
        }

        verify { repository wasNot Called }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun `skal beholde statusfeilen for overstyrt beregningsgrunnlag uten aa lagre`(feiltype: Int) {
        val behandlingId = randomUUID()
        val behandling =
            mockk<DetaljertBehandling> {
                every { status } returns BehandlingStatus.IVERKSATT
            }
        every { behandlingService.hentDetaljertBehandling(behandlingId, any()) } returns behandling
        val feil =
            when (feiltype) {
                0 -> TilstandException.UgyldigTilstand("Ugyldig status")
                1 -> TilstandException.IkkeFyltUt("Behandlingen er ikke fylt ut")
                2 -> TilstandException.KanIkkeRedigere("Behandlingen kan ikke redigeres")
                else -> BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.TRYGDETID_OPPDATERT)
            }
        every { behandlingStatusService.settTrygdetidOppdatert(behandlingId, any(), true) } throws feil
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            val response =
                client.post("/api/beregning/beregningsgrunnlag/$behandlingId/overstyr") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("""{"perioder":[]}""")
                }

            response.status shouldBe HttpStatusCode.BadRequest
            val respons = objectMapper.readTree(response.bodyAsText())
            respons["code"].asString() shouldBe "OVERSTYRT_BEREGNING_FEIL_BEHANDLINGSSTATUS"
            respons["meta"]["behandlingId"].asString() shouldBe behandlingId.toString()
            respons["meta"]["behandlingStatus"].asString() shouldBe "IVERKSATT"
        }

        verify { repository wasNot Called }
    }

    @Test
    fun `ugyldig overstyrt periode gir forstaaelig feil uten aa lagre`() {
        val behandlingId = randomUUID()
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, any(), any()) } returns true

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext()
                    beregningsGrunnlag(service, behandlingTilgangsSjekk)
                }

            val response =
                client.post("/api/beregning/beregningsgrunnlag/$behandlingId/overstyr") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    setBody(
                        """
                        {
                          "perioder": [{
                            "data": {
                              "utbetaltBeloep": "11379",
                              "foreldreloessats": false,
                              "trygdetid": 40,
                              "trygdetidForIdent": null,
                              "prorataBroekTeller": null,
                              "prorataBroekNevner": null,
                              "beskrivelse": "",
                              "aarsak": "AVKORTET_UFOERETRYGD"
                            },
                            "fom": "2026-05-01",
                            "tom": "2026-01-31"
                          }]
                        }
                        """.trimIndent(),
                    )
                }

            response.status shouldBe HttpStatusCode.BadRequest
            val feil = objectMapper.readTree(response.bodyAsText())
            feil["code"].asString() shouldBe "UGYLDIG_PERIODE_FOR_GRUNNLAG"
            feil["detail"].asString() shouldBe "Til og med må være lik eller etter Fra og med."
        }

        verify { repository wasNot Called }
        coVerify(exactly = 0) { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) }
    }

    @Test
    fun `skal returnere 204 naar beregnings ikke finnes`() {
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = randomSakId(),
                sakType = SakType.BARNEPENSJON,
                soeker = "diam",
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt = null,
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                utlandstilknytning = null,
                revurderingInfo = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
                opphoerFraOgMed = null,
                relatertBehandlingId = null,
                tidligereFamiliepleier = null,
                opprinnelse = BehandlingOpprinnelse.UKJENT,
            )

        every { repository.finnBeregningsGrunnlag(any()) } returns null

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            val response =
                client.get("/api/beregning/beregningsgrunnlag/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `skal hente og lagre beregningsgrunnlag for sist iverksatte hvis ikke noe finnes og det er revurdering`() {
        val idRevurdering = randomUUID()
        val idForrigeIverksatt = randomUUID()
        val sakId = randomSakId()

        coEvery { repository.finnOverstyrBeregningGrunnlagForBehandling(any()) } returns mockk(relaxed = true)
        coEvery { repository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) } just Runs
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingService.hentDetaljertBehandling(idRevurdering, any()) } returns barnepensjonRevurdering(sakId)
        coEvery {
            vedtaksvurderingService.hentIverksatteVedtakISak(sakId)
        } returns listOf(mockVedtak(idForrigeIverksatt, VedtakType.INNVILGELSE))
        coEvery { vedtaksvurderingService.hentInnvilgedePerioder(any<SakId>()) } returns emptyList()
        every { repository.finnBeregningsGrunnlag(idRevurdering) } returns null
        every { repository.finnBeregningsGrunnlag(idForrigeIverksatt) } returns
            BeregningsGrunnlag(
                behandlingId = idForrigeIverksatt,
                kilde =
                    Grunnlagsopplysning.Saksbehandler(
                        ident = "",
                        tidspunkt = Tidspunkt.now(),
                    ),
                soeskenMedIBeregning = emptyList(),
                institusjonsopphold = emptyList(),
                beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag(),
            )
        every { repository.lagreBeregningsGrunnlag(any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            val response =
                client.get("/api/beregning/beregningsgrunnlag/$idRevurdering") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
        }

        coVerify(exactly = 1) {
            vedtaksvurderingService.hentIverksatteVedtakISak(sakId)
            repository.finnBeregningsGrunnlag(idForrigeIverksatt)
            repository.lagreBeregningsGrunnlag(any())
        }
        verify(exactly = 2) {
            repository.finnBeregningsGrunnlag(idRevurdering)
        }
    }

    @Test
    fun `skal hente beregning`() {
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true

        val id = randomUUID()

        every { repository.finnBeregningsGrunnlag(any()) } returns
            BeregningsGrunnlag(
                behandlingId = id,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsopphold = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            val response =
                client.get("/api/beregning/beregningsgrunnlag/$id") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            val hentet = objectMapper.readValue(response.bodyAsText(), BeregningsGrunnlag::class.java)

            hentet.soeskenMedIBeregning shouldBe emptyList()
        }
    }

    @Test
    fun `skal returnere forbidden naar saksbehandler ikke har tilgang til behandling`() {
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns false

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            client
                .get("/api/beregning/beregningsgrunnlag/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let {
                    it.status shouldBe HttpStatusCode.Forbidden
                }
        }
    }

    @Test
    fun `skal returnere forbidden naar saksbehandler ikke har tilgang til behandling ved opprettelse`() {
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns false

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext()
                    beregningsGrunnlag(service, behandlingTilgangsSjekk)
                }

            client
                .post("/api/beregning/beregningsgrunnlag/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        LagreBeregningsGrunnlag(
                            emptyList(),
                            emptyList(),
                        ),
                    )
                }.let {
                    it.status shouldBe HttpStatusCode.Forbidden
                }
        }
    }

    @Test
    fun `skal opprettere`() {
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), true) } returns Unit
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) } returns Unit
        every { repository.finnBeregningsGrunnlag(any()) } returns mockk(relaxed = true)
        every { repository.lagreBeregningsGrunnlag(any()) } returns true
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagService.hentOpplysningsgrunnlag(any()) } returns hentOpplysningsgrunnlag
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = randomSakId(),
                sakType = SakType.BARNEPENSJON,
                soeker = "diam",
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        REFORM_TIDSPUNKT_BP,
                        kilde =
                            Grunnlagsopplysning.Saksbehandler(
                                ident = "",
                                tidspunkt = Tidspunkt.now(),
                            ),
                        "",
                    ),
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                utlandstilknytning = null,
                revurderingInfo = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
                opphoerFraOgMed = null,
                relatertBehandlingId = null,
                tidligereFamiliepleier = null,
                opprinnelse = BehandlingOpprinnelse.UKJENT,
            )

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext()
                    beregningsGrunnlag(service, behandlingTilgangsSjekk)
                }

            client
                .post("/api/beregning/beregningsgrunnlag/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        LagreBeregningsGrunnlag(
                            emptyList(),
                            emptyList(),
                        ),
                    )
                }.let {
                    it.status shouldBe HttpStatusCode.OK
                }
        }
    }

    @Test
    fun `skal returnere conflict fra opprettelse `() {
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), true) } returns Unit
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) } returns Unit
        every { repository.finnBeregningsGrunnlag(any()) } returns null
        every { repository.lagreBeregningsGrunnlag(any()) } returns false
        val hentOpplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { grunnlagService.hentOpplysningsgrunnlag(any()) } returns hentOpplysningsgrunnlag
        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = randomSakId(),
                sakType = SakType.BARNEPENSJON,
                soeker = "diam",
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        REFORM_TIDSPUNKT_BP,
                        kilde =
                            Grunnlagsopplysning.Saksbehandler(
                                ident = "",
                                tidspunkt = Tidspunkt.now(),
                            ),
                        "",
                    ),
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                utlandstilknytning = null,
                revurderingInfo = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
                opphoerFraOgMed = null,
                relatertBehandlingId = null,
                tidligereFamiliepleier = null,
                opprinnelse = BehandlingOpprinnelse.UKJENT,
            )

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext()
                    beregningsGrunnlag(service, behandlingTilgangsSjekk)
                }

            val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> =
                listOf(
                    GrunnlagMedPeriode(
                        fom = LocalDate.of(2022, 8, 1),
                        tom = null,
                        data =
                            listOf(
                                SoeskenMedIBeregning(HELSOESKEN_FOEDSELSNUMMER, true),
                            ),
                    ),
                )
            client
                .post("/api/beregning/beregningsgrunnlag/${randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        LagreBeregningsGrunnlag(
                            soeskenMedIBeregning,
                            emptyList(),
                        ),
                    )
                }.let {
                    it.status shouldBe HttpStatusCode.Conflict
                }
        }
    }

    @Test
    fun `skal duplisere hvis en saksbehandler kaller duplisering`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns mockk(relaxed = true)
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        coEvery { vedtaksvurderingService.hentInnvilgedePerioder(any<SakId>()) } returns emptyList()
        every { repository.finnBeregningsGrunnlag(forrige) } returns
            BeregningsGrunnlag(
                behandlingId = forrige,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsopphold = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.finnOverstyrBeregningGrunnlagForBehandling(any()) } returns emptyList()
        every { repository.finnBeregningsGrunnlag(nye) } returns null
        every { repository.lagreBeregningsGrunnlag(any()) } returns true
        coEvery { sanksjonService.kopierSanksjon(any(), any(), any()) } just Runs

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            client
                .post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let {
                    it.status shouldBe HttpStatusCode.NoContent
                }
        }
    }

    @Test
    fun `skal duplisere hvis en system bruker kaller duplisering`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns mockk(relaxed = true)
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        coEvery { vedtaksvurderingService.hentInnvilgedePerioder(any<SakId>()) } returns emptyList()
        every { repository.finnBeregningsGrunnlag(forrige) } returns
            BeregningsGrunnlag(
                behandlingId = forrige,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsopphold = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.finnOverstyrBeregningGrunnlagForBehandling(any()) } returns emptyList()
        every { repository.finnBeregningsGrunnlag(nye) } returns null
        every { repository.lagreBeregningsGrunnlag(any()) } returns true
        coEvery { sanksjonService.kopierSanksjon(any(), any(), any()) } just Runs

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            client
                .post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $systemToken")
                }.let {
                    it.status shouldBe HttpStatusCode.NoContent
                }
        }
    }

    @Test
    fun `skal feile hvis forrige beregningsgrunnlag ikke finnes`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        every { repository.finnBeregningsGrunnlag(forrige) } returns null
        every { repository.lagreBeregningsGrunnlag(any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            client
                .post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $systemToken")
                }.let {
                    it.status shouldBe HttpStatusCode.InternalServerError
                }
        }
    }

    @Test
    fun `skal feile hvis nye beregningsgrunnlag allerede finnes`() {
        val forrige = randomUUID()
        val nye = randomUUID()

        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
        coEvery { behandlingStatusService.settBeregnet(any(), any(), any()) } returns Unit
        every { repository.finnBeregningsGrunnlag(forrige) } returns
            BeregningsGrunnlag(
                behandlingId = forrige,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsopphold = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.finnBeregningsGrunnlag(nye) } returns
            BeregningsGrunnlag(
                behandlingId = nye,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                soeskenMedIBeregning = emptyList(),
                institusjonsopphold = emptyList(),
                beregningsMetode = BeregningsMetode.BEST.toGrunnlag(),
            )
        every { repository.lagreBeregningsGrunnlag(any()) } returns true

        testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                beregningsGrunnlag(service, behandlingTilgangsSjekk)
            }

            client
                .post("/api/beregning/beregningsgrunnlag/$nye/fra/$forrige") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $systemToken")
                }.let {
                    it.status shouldBe HttpStatusCode.InternalServerError
                }
        }
    }

    @Test
    fun `skal hente overstyr beregning grunnlag`() {
        val behandlingId = randomUUID()

        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, any(), any()) } returns true

        every { repository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12L),
                    datoTOM = LocalDate.now().minusYears(6L),
                    utbetaltBeloep = 123L,
                    foreldreloessats = false,
                    trygdetid = 10L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId1,
                    beskrivelse = "test periode 1",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6L),
                    datoTOM = null,
                    utbetaltBeloep = 456L,
                    foreldreloessats = false,
                    trygdetid = 20L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = 10,
                    prorataBroekNevner = 20,
                    sakId = sakId1,
                    beskrivelse = "test periode 2",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
            )

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext()
                    beregningsGrunnlag(service, behandlingTilgangsSjekk)
                }

            client
                .get("/api/beregning/beregningsgrunnlag/$behandlingId/overstyr") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { response ->
                    response.status shouldBe HttpStatusCode.OK

                    val grunnlag =
                        objectMapper.readValue(response.bodyAsText(), OverstyrBeregningGrunnlagDTO::class.java)

                    grunnlag.perioder.let { perioder ->
                        perioder.size shouldBe 2
                        perioder.minBy { it.fom }.let { periode ->
                            periode.fom shouldBe LocalDate.now().minusYears(12L)
                            periode.tom shouldBe LocalDate.now().minusYears(6L)
                            periode.data.utbetaltBeloep shouldBe 123L
                            periode.data.trygdetid shouldBe 10L
                        }
                        perioder.maxBy { it.fom }.let { periode ->
                            periode.fom shouldBe LocalDate.now().minusYears(6L)
                            periode.tom shouldBe null
                            periode.data.utbetaltBeloep shouldBe 456L
                            periode.data.trygdetid shouldBe 20L
                        }
                    }
                }
        }
    }

    @Test
    fun `skal kopiere overstyr beregning grunnlag fra forrige behandling naar grunnlag ikke finnes`() {
        val idRevurdering = randomUUID()
        val idForrigeIverksatt = randomUUID()
        val sakId = randomSakId()

        coEvery { beregningRepository.hentOverstyrBeregning(sakId) } returns overstyrBeregning(sakId)
        coEvery { behandlingService.hentDetaljertBehandling(idRevurdering, any()) } returns barnepensjonRevurdering(sakId)
        coEvery {
            vedtaksvurderingService.hentIverksatteVedtakISak(sakId)
        } returns listOf(mockVedtak(idForrigeIverksatt, VedtakType.INNVILGELSE))

        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(idRevurdering, any(), any()) } returns true

        every { repository.finnOverstyrBeregningGrunnlagForBehandling(idRevurdering) } returns emptyList()
        every { repository.finnOverstyrBeregningGrunnlagForBehandling(idForrigeIverksatt) } returns
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = idForrigeIverksatt,
                    datoFOM = LocalDate.now().minusYears(12L),
                    datoTOM = LocalDate.now().minusYears(6L),
                    utbetaltBeloep = 123L,
                    foreldreloessats = false,
                    trygdetid = 10L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId1,
                    beskrivelse = "test periode 1",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = idForrigeIverksatt,
                    datoFOM = LocalDate.now().minusYears(6L),
                    datoTOM = null,
                    utbetaltBeloep = 456L,
                    foreldreloessats = false,
                    trygdetid = 20L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = 10,
                    prorataBroekNevner = 20,
                    sakId = sakId1,
                    beskrivelse = "test periode 2",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
            )
        coEvery { repository.lagreOverstyrBeregningGrunnlagForBehandling(any(), any()) } just runs

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext()
                    beregningsGrunnlag(service, behandlingTilgangsSjekk)
                }

            client
                .get("/api/beregning/beregningsgrunnlag/$idRevurdering/overstyr") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.let { response ->
                    response.status shouldBe HttpStatusCode.OK

                    val grunnlag =
                        objectMapper.readValue(response.bodyAsText(), OverstyrBeregningGrunnlagDTO::class.java)

                    grunnlag.perioder.let { perioder ->
                        perioder.size shouldBe 2
                        perioder.minBy { it.fom }.let { periode ->
                            periode.fom shouldBe LocalDate.now().minusYears(12L)
                            periode.tom shouldBe LocalDate.now().minusYears(6L)
                            periode.data.utbetaltBeloep shouldBe 123L
                            periode.data.trygdetid shouldBe 10L
                        }
                        perioder.maxBy { it.fom }.let { periode ->
                            periode.fom shouldBe LocalDate.now().minusYears(6L)
                            periode.tom shouldBe null
                            periode.data.utbetaltBeloep shouldBe 456L
                            periode.data.trygdetid shouldBe 20L
                        }
                    }
                }
        }
    }

    @Test
    fun `skal lagre overstyr beregning grunnlag`() {
        val behandlingId = randomUUID()
        val slot = slot<List<OverstyrBeregningGrunnlagDao>>()
        val randomSakId = randomSakId()

        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, any(), any()) } returns true

        coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns
            DetaljertBehandling(
                id = randomUUID(),
                sak = randomSakId,
                sakType = SakType.BARNEPENSJON,
                soeker = "diam",
                status = BehandlingStatus.TRYGDETID_OPPDATERT,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt = null,
                revurderingsaarsak = null,
                prosesstype = Prosesstype.MANUELL,
                boddEllerArbeidetUtlandet = null,
                utlandstilknytning = null,
                revurderingInfo = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = true,
                opphoerFraOgMed = null,
                relatertBehandlingId = null,
                tidligereFamiliepleier = null,
                opprinnelse = BehandlingOpprinnelse.UKJENT,
            )

        every { repository.lagreOverstyrBeregningGrunnlagForBehandling(behandlingId, capture(slot)) } just runs

        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), true) } returns Unit
        coEvery { behandlingStatusService.settTrygdetidOppdatert(any(), any(), any()) } returns Unit

        every { repository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId) } returns
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12L),
                    datoTOM = LocalDate.now().minusYears(6L),
                    utbetaltBeloep = 123L,
                    foreldreloessats = false,
                    trygdetid = 10L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = randomSakId,
                    beskrivelse = "test periode 1",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
                OverstyrBeregningGrunnlagDao(
                    id = randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6L),
                    datoTOM = null,
                    utbetaltBeloep = 456L,
                    foreldreloessats = false,
                    trygdetid = 20L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = randomSakId,
                    beskrivelse = "test periode 2",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
            )

        testApplication {
            val client =
                runServer(mockOAuth2Server) {
                    attachMockContext()
                    beregningsGrunnlag(service, behandlingTilgangsSjekk)
                }

            client
                .post("/api/beregning/beregningsgrunnlag/$behandlingId/overstyr") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(
                        OverstyrBeregningGrunnlag(
                            perioder =
                                listOf(
                                    GrunnlagMedPeriode(
                                        data =
                                            OverstyrBeregningGrunnlagData(
                                                utbetaltBeloep = 123L,
                                                foreldreloessats = false,
                                                trygdetid = 10L,
                                                trygdetidForIdent = null,
                                                prorataBroekTeller = null,
                                                prorataBroekNevner = null,
                                                beskrivelse = "test periode 1",
                                                aarsak = "ANNET",
                                            ),
                                        fom = LocalDate.now().minusYears(12L),
                                        tom = LocalDate.now().minusYears(6L),
                                    ),
                                    GrunnlagMedPeriode(
                                        data =
                                            OverstyrBeregningGrunnlagData(
                                                utbetaltBeloep = 456L,
                                                foreldreloessats = false,
                                                trygdetid = 20L,
                                                trygdetidForIdent = null,
                                                prorataBroekTeller = null,
                                                prorataBroekNevner = null,
                                                beskrivelse = "test periode 2",
                                                aarsak = "ANNET",
                                            ),
                                        fom = LocalDate.now().minusYears(6L),
                                        tom = null,
                                    ),
                                ),
                            kilde =
                                Grunnlagsopplysning.Saksbehandler(
                                    ident = "Z123456",
                                    Tidspunkt.now(),
                                ),
                        ),
                    )
                }.let { response ->
                    response.status shouldBe HttpStatusCode.OK

                    val grunnlag = objectMapper.readValue(response.bodyAsText(), OverstyrBeregningGrunnlag::class.java)

                    grunnlag.perioder.let { perioder ->
                        perioder.size shouldBe 2
                        perioder.minBy { it.fom }.let { periode ->
                            periode.fom shouldBe LocalDate.now().minusYears(12L)
                            periode.tom shouldBe LocalDate.now().minusYears(6L)
                            periode.data.utbetaltBeloep shouldBe 123L
                            periode.data.trygdetid shouldBe 10L
                        }
                        perioder.maxBy { it.fom }.let { periode ->
                            periode.fom shouldBe LocalDate.now().minusYears(6L)
                            periode.tom shouldBe null
                            periode.data.utbetaltBeloep shouldBe 456L
                            periode.data.trygdetid shouldBe 20L
                        }
                    }

                    slot.captured.let { daoList ->
                        daoList.size shouldBe 2

                        daoList.minBy { it.datoFOM }.let { dao ->
                            dao.datoFOM shouldBe LocalDate.now().minusYears(12L)
                            dao.datoTOM shouldBe LocalDate.now().minusYears(6L)
                            dao.utbetaltBeloep shouldBe 123L
                            dao.trygdetid shouldBe 10L
                            dao.sakId shouldBe randomSakId
                        }
                        daoList.maxBy { it.datoFOM }.let { dao ->
                            dao.datoFOM shouldBe LocalDate.now().minusYears(6L)
                            dao.datoTOM shouldBe null
                            dao.utbetaltBeloep shouldBe 456L
                            dao.trygdetid shouldBe 20L
                            dao.sakId shouldBe randomSakId
                        }
                    }
                }
        }
    }

    private fun barnepensjonRevurdering(sakId: SakId): DetaljertBehandling =
        DetaljertBehandling(
            id = randomUUID(),
            sak = sakId,
            sakType = SakType.BARNEPENSJON,
            soeker = "",
            status = BehandlingStatus.TRYGDETID_OPPDATERT,
            behandlingType = BehandlingType.REVURDERING,
            virkningstidspunkt = mockk(),
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            boddEllerArbeidetUtlandet = null,
            utlandstilknytning = null,
            revurderingInfo = null,
            vedtaksloesning = Vedtaksloesning.GJENNY,
            sendeBrev = true,
            opphoerFraOgMed = null,
            relatertBehandlingId = null,
            tidligereFamiliepleier = null,
            opprinnelse = BehandlingOpprinnelse.UKJENT,
        )

    private fun mockVedtak(
        behandlingId: UUID,
        type: VedtakType,
    ) = vedtak(behandlingId = behandlingId, type = type)

    private fun overstyrBeregning(sakId: SakId): OverstyrBeregning =
        OverstyrBeregning(
            sakId = sakId,
            beskrivelse = "",
            tidspunkt = Tidspunkt.now(),
            status = OverstyrBeregningStatus.AKTIV,
            kategori = OverstyrtBeregningKategori.AVKORTING_UFOERETRYGD,
        )

    private val token: String by lazy { mockOAuth2Server.issueSaksbehandlerToken() }

    private val systemToken: String by lazy { mockOAuth2Server.issueSystembrukerToken() }
}
