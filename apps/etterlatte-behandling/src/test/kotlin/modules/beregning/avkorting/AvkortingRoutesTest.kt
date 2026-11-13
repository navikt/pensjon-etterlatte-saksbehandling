package no.nav.etterlatte.modules.beregning.avkorting

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.attachMockContext
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling.BehandlingStoetterIkkeStatusEndringException
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.beregning.AarligInntektsjusteringAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagFlereInntekterDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.ForventetInntektDto
import no.nav.etterlatte.libs.common.beregning.MottattInntektsjusteringAvkortigRequest
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.modules.beregning.avkorting.inntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.modules.beregning.avkorting.inntektsjustering.MottattInntektsjusteringService
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.modules.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.sak
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvkortingRoutesTest {
    private val mockOAuth2Server = MockOAuth2Server()
    private val behandlingTilgangsSjekk = mockk<BehandlingTilgangsSjekk>()
    private val avkortingService = mockk<AvkortingService>()
    private val avkortingTidligAlderspensjonService = mockk<AvkortingTidligAlderspensjonService>()
    private val aarligInntektsjusteringService = mockk<AarligInntektsjusteringService>()
    private val mottattInntektsjusteringService = mockk<MottattInntektsjusteringService>()
    private val etteroppgjoerService = mockk<EtteroppgjoerService>()

    @BeforeAll
    fun beforeAll() {
        mockOAuth2Server.startRandomPort()
    }

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            behandlingTilgangsSjekk,
            avkortingService,
            avkortingTidligAlderspensjonService,
            aarligInntektsjusteringService,
            mottattInntektsjusteringService,
            etteroppgjoerService,
        )
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(any(), any(), any()) } returns true
    }

    @AfterAll
    fun afterAll() {
        mockOAuth2Server.shutdown()
    }

    @Test
    fun `skal returnere 204 naar avkorting ikke finnes`() {
        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(any(), any()) } returns null

        testApplication {
            val response =
                client.get("/api/beregning/avkorting/${UUID.randomUUID()}") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `skal motta og returnere avkorting`() {
        val behandlingsId = UUID.randomUUID()
        val avkortingsgrunnlagId = UUID.randomUUID()
        val dato = YearMonth.of(2023, 1)
        val tidspunkt = Tidspunkt.now()
        val avkortingsgrunnlag =
            avkortinggrunnlag(
                id = avkortingsgrunnlagId,
                periode = Periode(fom = dato, tom = dato),
                kilde = Grunnlagsopplysning.Saksbehandler("Saksbehandler01", tidspunkt),
            )
        val avkortetYtelseId = UUID.randomUUID()
        val avkorting =
            AvkortingFrontendDto(
                redigerbareInntekter =
                    listOf(
                        ForventetInntektDto(
                            id = avkortingsgrunnlagId,
                            fom = dato,
                            tom = dato,
                            inntektTom = 100000,
                            fratrekkInnAar = 10000,
                            spesifikasjon = "Spesifikasjon",
                            inntektUtlandTom = 0,
                            fratrekkInnAarUtland = 0,
                            innvilgaMaaneder = 12,
                            inntektInnvilgetPeriode = 90000,
                            kilde =
                                AvkortingGrunnlagKildeDto(
                                    tidspunkt = tidspunkt.toString(),
                                    ident = "Saksbehandler01",
                                ),
                        ),
                    ),
                avkortingGrunnlag =
                    listOf(
                        ForventetInntektDto(
                            id = avkortingsgrunnlagId,
                            fom = dato,
                            tom = dato,
                            inntektTom = 100000,
                            fratrekkInnAar = 10000,
                            spesifikasjon = "Spesifikasjon",
                            inntektUtlandTom = 0,
                            fratrekkInnAarUtland = 0,
                            innvilgaMaaneder = 12,
                            inntektInnvilgetPeriode = 90000,
                            kilde =
                                AvkortingGrunnlagKildeDto(
                                    tidspunkt = tidspunkt.toString(),
                                    ident = "Saksbehandler01",
                                ),
                        ),
                    ),
                avkortetYtelse =
                    listOf(
                        AvkortetYtelseDto(
                            id = avkortetYtelseId,
                            type = AvkortetYtelseType.AARSOPPGJOER.name,
                            fom = dato,
                            tom = dato,
                            ytelseFoerAvkorting = 300,
                            avkortingsbeloep = 200,
                            ytelseEtterAvkorting = 50,
                            restanse = 50,
                            sanksjon = null,
                        ),
                    ),
                tidligereAvkortetYtelse =
                    listOf(
                        AvkortetYtelseDto(
                            id = avkortetYtelseId,
                            type = AvkortetYtelseType.AARSOPPGJOER.name,
                            fom = dato,
                            tom = dato,
                            ytelseFoerAvkorting = 300,
                            avkortingsbeloep = 200,
                            ytelseEtterAvkorting = 50,
                            restanse = 50,
                            sanksjon = null,
                        ),
                    ),
            )
        coEvery { avkortingService.beregnAvkortingMedNyeGrunnlag(any(), any(), any()) } returns avkorting

        testApplication {
            val response =
                client.post("/api/beregning/avkorting/$behandlingsId/liste") {
                    val request =
                        with(avkorting.redigerbareInntekter.singleOrNull()!!) {
                            AvkortingGrunnlagFlereInntekterDto(
                                inntekter =
                                    listOf(
                                        AvkortingGrunnlagLagreDto(
                                            id = id,
                                            inntektTom = inntektTom,
                                            fratrekkInnAar = fratrekkInnAar,
                                            inntektUtlandTom = inntektUtlandTom,
                                            fratrekkInnAarUtland = fratrekkInnAarUtland,
                                            spesifikasjon = spesifikasjon,
                                            fom = dato,
                                        ),
                                    ),
                            )
                        }
                    setBody(request.toJson())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
            val result = objectMapper.readValue(response.bodyAsText(), AvkortingFrontendDto::class.java)
            result shouldBe avkorting
            coVerify {
                avkortingService.beregnAvkortingMedNyeGrunnlag(
                    behandlingsId,
                    withArg {
                        it.size shouldBe 1
                        it[0].inntektTom shouldBe avkortingsgrunnlag.inntektTom
                        it[0].fratrekkInnAar shouldBe avkortingsgrunnlag.fratrekkInnAar
                        it[0].spesifikasjon shouldBe avkortingsgrunnlag.spesifikasjon
                    },
                    any(),
                )
            }
        }
    }

    @Test
    fun `skal beholde lesetilgang ved henting av avkorting`() {
        val behandlingId = UUID.randomUUID()
        val avkorting = AvkortingFrontendDto(emptyList(), emptyList(), emptyList(), emptyList())
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, false, any()) } returns true
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) } returns false
        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, any()) } returns avkorting

        testApplication {
            val response =
                client.get("/api/beregning/avkorting/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
            objectMapper.readValue(response.bodyAsText(), AvkortingFrontendDto::class.java) shouldBe avkorting
        }

        coVerify(exactly = 1) { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, false, any()) }
        coVerify(exactly = 0) { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) }
        coVerify(exactly = 1) { avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, any()) }
    }

    @Test
    fun `skal returnere 204 til lesebruker naar avkorting ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, false, any()) } returns true
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) } returns false
        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, any()) } returns null

        testApplication {
            val response =
                client.get("/api/beregning/avkorting/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.NoContent
        }

        coVerify(exactly = 1) { avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, any()) }
        coVerify(exactly = 0) { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) }
    }

    @Test
    fun `skal avvise henting uten lesetilgang`() {
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, false, any()) } returns false

        testApplication {
            val response =
                client.get("/api/beregning/avkorting/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.Forbidden
        }

        coVerify(exactly = 0) {
            behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any())
            avkortingService.hentOpprettEllerReberegnAvkorting(any(), any())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal hente avkorting for systembruker og saksbehandler`(systembruker: Boolean) {
        val behandlingId = UUID.randomUUID()
        val avkorting = AvkortingFrontendDto(emptyList(), emptyList(), emptyList(), emptyList())
        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, any()) } returns avkorting

        testApplication {
            val token =
                if (systembruker) {
                    mockOAuth2Server.issueSystembrukerToken()
                } else {
                    mockOAuth2Server.issueSaksbehandlerToken()
                }
            val response =
                client.get("/api/beregning/avkorting/$behandlingId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }

            response.status shouldBe HttpStatusCode.OK
            objectMapper.readValue(response.bodyAsText(), AvkortingFrontendDto::class.java) shouldBe avkorting
        }

        coVerify(exactly = 1) { avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, any()) }
        coVerify(exactly = 0) {
            behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any())
        }
    }

    @Test
    fun `skal fortsatt avvise endring av inntekter uten skrivetilgang`() {
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, false, any()) } returns true
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) } returns false

        testApplication {
            val response =
                client.post("/api/beregning/avkorting/$behandlingId/liste") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(AvkortingGrunnlagFlereInntekterDto(emptyList()).toJson())
                }

            response.status shouldBe HttpStatusCode.Forbidden
        }

        coVerify(exactly = 0) { avkortingService.beregnAvkortingMedNyeGrunnlag(any(), any(), any()) }
        coVerify(exactly = 0) { avkortingService.hentOpprettEllerReberegnAvkorting(any(), any()) }
    }

    @Test
    fun `skal fortsatt tillate lesetilgang til manglende inntektsaar`() {
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, false, any()) } returns true
        coEvery { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, true, any()) } returns false
        coEvery { avkortingService.manglendeInntektsaar(behandlingId, any(), any()) } returns emptyList()

        testApplication {
            val response =
                client.get("/api/beregning/avkorting/$behandlingId/manglende-inntektsaar") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
        }

        coVerify(exactly = 1) { behandlingTilgangsSjekk.harTilgangTilBehandling(behandlingId, false, any()) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/00000000-0000-0000-0000-000000000001/haandter-tidlig-alderspensjon",
            "/aarlig-inntektsjustering",
            "/mottatt-inntektsjustering",
        ],
    )
    fun `skal avvise saksbehandler fra systemendepunktene`(sti: String) {
        testApplication {
            val response =
                client.post("/api/beregning/avkorting$sti") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                }

            response.status shouldBe HttpStatusCode.NotFound
        }

        coVerify(exactly = 0) {
            avkortingTidligAlderspensjonService.opprettOppgaveHvisTidligAlderspensjon(any(), any())
            aarligInntektsjusteringService.kopierAarligInntektsjustering(any(), any(), any(), any())
            mottattInntektsjusteringService.opprettAvkortingMedBrukeroppgittInntekt(any(), any())
        }
    }

    @Test
    fun `skal fortsatt opprette oppgave for systembruker`() {
        val behandlingId = UUID.randomUUID()
        coEvery { avkortingTidligAlderspensjonService.opprettOppgaveHvisTidligAlderspensjon(behandlingId, any()) } returns Unit

        testApplication {
            val response =
                client.post("/api/beregning/avkorting/$behandlingId/haandter-tidlig-alderspensjon") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSystembrukerToken()}")
                }

            response.status shouldBe HttpStatusCode.OK
        }

        coVerify(exactly = 1) { avkortingTidligAlderspensjonService.opprettOppgaveHvisTidligAlderspensjon(behandlingId, any()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun `skal returnere opprinnelig statusfeil fra direkte servicekall`(feiltype: Int) {
        val behandlingId = UUID.randomUUID()
        val behandlingStatusService = mockk<BehandlingStatusService>()
        val feil =
            when (feiltype) {
                0 -> TilstandException.UgyldigTilstand("Ugyldig status")
                1 -> TilstandException.IkkeFyltUt("Behandlingen er ikke fylt ut")
                2 -> TilstandException.KanIkkeRedigere("Behandlingen kan ikke redigeres")
                else -> BehandlingStoetterIkkeStatusEndringException(BehandlingStatus.AVKORTET)
            }
        every { behandlingStatusService.settAvkortet(behandlingId, any(), true) } throws feil
        val service =
            AvkortingService(
                behandlingService = mockk(),
                avkortingRepository = mockk(),
                beregningService = mockk(),
                sanksjonService = mockk(),
                aldersovergangService = mockk(),
                vedtaksvurderingService = mockk(),
                avkortingReparerAarsoppgjoeret = mockk(),
                featureToggleService = mockk(),
                behandlingStatusService = behandlingStatusService,
                behandlingTilgangsSjekk = behandlingTilgangsSjekk,
            )

        testApplication(avkortingService = service) {
            val response =
                client.post("/api/beregning/avkorting/$behandlingId/liste") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(AvkortingGrunnlagFlereInntekterDto(emptyList()).toJson())
                }

            response.status shouldBe HttpStatusCode.Forbidden
            objectMapper.readTree(response.bodyAsText())["code"].asString() shouldBe "BEHANDLING_FEIL_STATUS_FOR_AVKORTING"
        }

        verify(exactly = 1) { behandlingStatusService.settAvkortet(behandlingId, any(), true) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal ikke oversette feil fra hele avkortingsoperasjonen`(kopier: Boolean) {
        val behandlingId = UUID.randomUUID()
        val forrigeBehandlingId = UUID.randomUUID()
        val feil = TilstandException.UgyldigTilstand("Ugyldig status")
        coEvery { avkortingService.hentOpprettEllerReberegnAvkorting(behandlingId, any()) } throws feil
        coEvery { avkortingService.kopierAvkorting(behandlingId, forrigeBehandlingId, any()) } throws feil

        testApplication {
            val response =
                if (kopier) {
                    client.post("/api/beregning/avkorting/$behandlingId/med/$forrigeBehandlingId") {
                        header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    }
                } else {
                    client.get("/api/beregning/avkorting/$behandlingId") {
                        header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    }
                }

            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal ikke oversette feil fra hele inntektsjusteringen`(aarlig: Boolean) {
        val behandlingId = UUID.randomUUID()
        val feil = TilstandException.UgyldigTilstand("Ugyldig status")
        coEvery { aarligInntektsjusteringService.kopierAarligInntektsjustering(any(), behandlingId, any(), any()) } throws feil
        coEvery { mottattInntektsjusteringService.opprettAvkortingMedBrukeroppgittInntekt(any(), any()) } throws feil
        val request =
            if (aarlig) {
                AarligInntektsjusteringAvkortingRequest(
                    aar = 2026,
                    forrigeBehandling = UUID.randomUUID(),
                    nyBehandling = behandlingId,
                )
            } else {
                MottattInntektsjusteringAvkortigRequest(
                    behandlingId = behandlingId,
                    virkningstidspunkt = YearMonth.of(2026, 1),
                    mottattInntektsjustering =
                        MottattInntektsjustering(
                            sak = SakId(1),
                            inntektsjusteringId = UUID.randomUUID(),
                            journalpostId = "journalpost",
                            mottattDato = LocalDateTime.of(2026, 1, 1, 0, 0),
                            inntektsaar = 2026,
                            arbeidsinntekt = 0,
                            naeringsinntekt = 0,
                            afpInntekt = null,
                            inntektFraUtland = 0,
                            datoForAaGaaAvMedAlderspensjon = null,
                        ),
                )
            }
        val sti = if (aarlig) "aarlig-inntektsjustering" else "mottatt-inntektsjustering"

        testApplication {
            val response =
                client.post("/api/beregning/avkorting/$sti") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSystembrukerToken()}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request.toJson())
                }

            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `skal avvise henting av etteroppgjoer fra en annen sak`() {
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val sakId = SakId(1L)
        val forbehandlingService = mockk<EtteroppgjoerForbehandlingService>()
        val behandlingService = mockk<BehandlingService>()
        val avkortingRepository = mockk<AvkortingRepository>()
        every { forbehandlingService.hentForbehandling(forbehandlingId) } returns
            mockk { every { sak } returns sak(sakId = sakId) }
        every { behandlingService.hentBehandling(behandlingId) } returns foerstegangsbehandling(sakId = SakId(2L))
        val service =
            EtteroppgjoerService(
                avkortingRepository = avkortingRepository,
                sanksjonService = mockk(),
                etteroppgjoerRepository = mockk(),
                avkortingService = avkortingService,
                reparerAarsoppgjoeret = mockk(),
                vedtakEtteroppgjoerService = mockk(),
                behandlingService = behandlingService,
                featureToggleService = mockk(),
                forbehandlingService = forbehandlingService,
            )

        testApplication(etteroppgjoerService = service) {
            val response =
                client.post("/api/beregning/avkorting/etteroppgjoer/hent") {
                    header(HttpHeaders.Authorization, "Bearer ${mockOAuth2Server.issueSaksbehandlerToken()}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(EtteroppgjoerBeregnetAvkortingRequest(forbehandlingId, behandlingId, 2024, sakId).toJson())
                }

            response.status shouldBe HttpStatusCode.BadRequest
            objectMapper.readTree(response.bodyAsText())["code"].asString() shouldBe "BEHANDLINGER_TILHOERER_IKKE_SAMME_SAK"
        }

        coVerify(exactly = 1) { behandlingTilgangsSjekk.harTilgangTilBehandling(forbehandlingId, false, any()) }
        verify(exactly = 0) {
            avkortingService.hentAvkortingMedReparertAarsoppgjoer(any(), any(), any())
            avkortingRepository.hentAvkorting(any())
        }
    }

    private fun testApplication(
        avkortingService: AvkortingService = this.avkortingService,
        etteroppgjoerService: EtteroppgjoerService = this.etteroppgjoerService,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        io.ktor.server.testing.testApplication {
            runServer(mockOAuth2Server) {
                attachMockContext()
                avkorting(
                    avkortingService,
                    behandlingTilgangsSjekk,
                    avkortingTidligAlderspensjonService,
                    aarligInntektsjusteringService,
                    mottattInntektsjusteringService,
                    etteroppgjoerService,
                )
            }
            block(this)
        }
    }
}
