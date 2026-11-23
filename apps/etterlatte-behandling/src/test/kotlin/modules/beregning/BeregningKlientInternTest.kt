package no.nav.etterlatte.modules.beregning

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.beregning.MaanederMedGammelSanksjonIAvkorting
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.IkkeTilgangTilBehandlingException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingService
import no.nav.etterlatte.modules.beregning.avkorting.inntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.modules.beregning.etteroppgjoer.BeregnetEtteroppgjoerResultat
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.modules.beregning.grunnbeloep.GrunnbeloepService
import no.nav.etterlatte.modules.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.modules.beregning.regler.beregning
import no.nav.etterlatte.modules.beregning.regler.sanksjon
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import no.nav.etterlatte.modules.beregning.ytelseMedGrunnlag.BeregningOgAvkortingBrevService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import java.util.UUID

class BeregningKlientInternTest {
    private val beregningService = mockk<BeregningService>()
    private val avkortingService = mockk<AvkortingService>()
    private val grunnlagService = mockk<BeregningsGrunnlagService>()
    private val sanksjonService = mockk<SanksjonService>()
    private val grunnbeloepService = mockk<GrunnbeloepService>()
    private val inntektsjusteringService = mockk<AarligInntektsjusteringService>()
    private val etteroppgjoerService = mockk<EtteroppgjoerService>()
    private val brevService = mockk<BeregningOgAvkortingBrevService>()
    private val tilgang = mockk<BehandlingTilgangsSjekk>()
    private val klient =
        BeregningKlientIntern(
            beregningService = { beregningService },
            avkortingService = { avkortingService },
            beregningsGrunnlagService = { grunnlagService },
            sanksjonService = { sanksjonService },
            grunnbeloepService = { grunnbeloepService },
            aarligInntektsjusteringService = { inntektsjusteringService },
            etteroppgjoerService = { etteroppgjoerService },
            beregningOgAvkortingBrevService = { brevService },
            behandlingTilgangsSjekk = tilgang,
        )
    private val behandlingId = UUID.randomUUID()
    private val forrigeBehandlingId = UUID.randomUUID()
    private val sakId = SakId(123)
    private val bruker = HardkodaSystembruker.testdata
    private val aar = 2025

    @Test
    fun `henter beregning og avkorting til brev`() =
        runBlocking<Unit> {
            val dto = mockk<BeregningOgAvkortingDto>()
            every { brevService.hentBeregningOgAvkorting(behandlingId, bruker) } returns dto

            klient.hentBeregningOgAvkorting(behandlingId, bruker) shouldBe dto
        }

    @Test
    fun `manglende ytelse til brev gir feil`() =
        runBlocking<Unit> {
            every { brevService.hentBeregningOgAvkorting(behandlingId, bruker) } returns null

            assertThrows<GenerellIkkeFunnetException> { klient.hentBeregningOgAvkorting(behandlingId, bruker) }
        }

    @Test
    fun `sletter avkorting`() =
        runBlocking<Unit> {
            every { avkortingService.slettAvkorting(behandlingId) } returns Unit

            klient.slettAvkorting(behandlingId, bruker)

            verify(exactly = 1) { avkortingService.slettAvkorting(behandlingId) }
        }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `henter overstyrt beregning og sjekker om den finnes`(overstyrt: Boolean) =
        runBlocking<Unit> {
            val beregning =
                OverstyrBeregning(
                    sakId = sakId,
                    beskrivelse = "Overstyrt beregning",
                    tidspunkt = Tidspunkt.now(),
                    kategori = OverstyrtBeregningKategori.UKJENT_AVDOED,
                ).takeIf { overstyrt }
            every { beregningService.hentOverstyrBeregningPaaBehandlingId(behandlingId, bruker) } returns beregning

            klient.harOverstyrt(behandlingId, bruker) shouldBe overstyrt
            klient.hentOverstyrtBeregning(behandlingId, bruker) shouldBe beregning?.toDTO()
        }

    @Test
    fun `henter gjeldende grunnbeloep`() =
        runBlocking<Unit> {
            val grunnbeloep = mockk<Grunnbeloep>()
            every { grunnbeloepService.hentGrunnbeloep(YearMonth.now()) } returns grunnbeloep

            klient.hentGrunnbeloep(bruker) shouldBe grunnbeloep
        }

    @Test
    fun `sjekker aarlig inntektsjustering`() =
        runBlocking<Unit> {
            val request = InntektsjusteringAvkortingInfoRequest(sakId, aar, behandlingId)
            val response = mockk<InntektsjusteringAvkortingInfoResponse>()
            every { inntektsjusteringService.hentSjekkAarligInntektsjustering(request) } returns response

            klient.inntektsjusteringAvkortingInfoSjekk(sakId, aar, behandlingId, bruker) shouldBe response
        }

    @Test
    fun `henter avkorting for etteroppgjoer`() =
        runBlocking<Unit> {
            val request = EtteroppgjoerBeregnetAvkortingRequest(behandlingId, forrigeBehandlingId, aar, sakId)
            val dto = mockk<EtteroppgjoerBeregnetAvkorting>()
            every {
                etteroppgjoerService.hentBeregnetAvkorting(behandlingId, forrigeBehandlingId, sakId, aar, bruker)
            } returns dto

            klient.hentAvkortingForForbehandlingEtteroppgjoer(request, bruker) shouldBe dto
        }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `henter beregnet etteroppgjoerresultat som kan mangle`(finnes: Boolean) =
        runBlocking<Unit> {
            val request = EtteroppgjoerHentBeregnetResultatRequest(aar, behandlingId, forrigeBehandlingId)
            val dto = mockk<BeregnetEtteroppgjoerResultatDto>()
            val resultat = mockk<BeregnetEtteroppgjoerResultat> { every { toDto() } returns dto }
            every {
                etteroppgjoerService.hentBeregnetEtteroppgjoerResultat(behandlingId, forrigeBehandlingId, aar)
            } returns resultat.takeIf { finnes }

            klient.hentBeregnetEtteroppgjoerResultat(request, bruker) shouldBe dto.takeIf { finnes }
        }

    @Test
    fun `beregner avkorting foer etteroppgjoerresultatet beregnes og lagres`() =
        runBlocking<Unit> {
            val request = faktiskInntektRequest()
            val dto = mockk<BeregnetEtteroppgjoerResultatDto>()
            val resultat = mockk<BeregnetEtteroppgjoerResultat> { every { toDto() } returns dto }
            justRun { etteroppgjoerService.beregnAvkortingForbehandling(request, bruker) }
            every {
                etteroppgjoerService.beregnOgLagreEtteroppgjoerResultat(
                    behandlingId,
                    forrigeBehandlingId,
                    aar,
                    request.harDoedsfall,
                    request.sammenlignTilOgMedBehandlingId,
                )
            } returns resultat

            klient.beregnAvkortingFaktiskInntekt(request, bruker) shouldBe dto

            verifyOrder {
                etteroppgjoerService.beregnAvkortingForbehandling(request, bruker)
                etteroppgjoerService.beregnOgLagreEtteroppgjoerResultat(
                    behandlingId,
                    forrigeBehandlingId,
                    aar,
                    request.harDoedsfall,
                    request.sammenlignTilOgMedBehandlingId,
                )
                resultat.toDto()
            }
        }

    @Test
    fun `kopierer beregningsgrunnlag fra forrige behandling`() =
        runBlocking<Unit> {
            justRun { grunnlagService.dupliserBeregningsGrunnlag(behandlingId, forrigeBehandlingId, bruker) }

            klient.opprettBeregningsgrunnlagFraForrigeBehandling(behandlingId, forrigeBehandlingId, bruker)

            verify(exactly = 1) { grunnlagService.dupliserBeregningsGrunnlag(behandlingId, forrigeBehandlingId, bruker) }
        }

    @Test
    fun `beregner behandling`() =
        runBlocking<Unit> {
            coEvery { beregningService.opprettBeregning(behandlingId, bruker) } returns beregning()

            klient.beregnBehandling(behandlingId, bruker)

            coVerify(exactly = 1) { beregningService.opprettBeregning(behandlingId, bruker) }
        }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `henter sanksjoner og returnerer null naar listen er tom`(harSanksjon: Boolean) =
        runBlocking<Unit> {
            val sanksjoner = listOf(sanksjon(behandlingId = behandlingId)).filter { harSanksjon }
            every { sanksjonService.hentSanksjon(behandlingId) } returns sanksjoner

            klient.hentSanksjoner(behandlingId, bruker) shouldBe sanksjoner.takeIf { harSanksjon }
        }

    @Test
    fun `henter eller kopierer beregningsgrunnlag`() =
        runBlocking<Unit> {
            val grunnlag = mockk<BeregningsGrunnlag>()
            coEvery { grunnlagService.hentEllerKopierBeregningsGrunnlag(behandlingId, bruker) } returns grunnlag

            klient.hentBeregningsgrunnlag(behandlingId, bruker) shouldBe grunnlag
        }

    @Test
    fun `manglende beregningsgrunnlag gir feil`() =
        runBlocking<Unit> {
            coEvery { grunnlagService.hentEllerKopierBeregningsGrunnlag(behandlingId, bruker) } returns null

            assertThrows<BeregningsgrunnlagMangler> { klient.hentBeregningsgrunnlag(behandlingId, bruker) }
        }

    @Test
    fun `henter beregning som DTO`() =
        runBlocking<Unit> {
            val beregning = beregning()
            every { beregningService.hentBeregning(behandlingId, bruker) } returns beregning

            klient.hentBeregning(behandlingId, bruker) shouldBe beregning.toDTO()
        }

    @Test
    fun `manglende beregning gir feil`() =
        runBlocking<Unit> {
            every { beregningService.hentBeregning(behandlingId, bruker) } returns null

            assertThrows<GenerellIkkeFunnetException> { klient.hentBeregning(behandlingId, bruker) }
        }

    @Test
    fun `henter fullfoert avkorting uten aa beregne paa nytt`() =
        runBlocking<Unit> {
            val dto = mockk<AvkortingDto>()
            every { avkortingService.hentFullfoertAvkorting(behandlingId, bruker) } returns dto

            klient.hentAvkorting(behandlingId, bruker) shouldBe dto
        }

    @Test
    fun `sjekker sanksjon etter gamle regler for valgt aar`() =
        runBlocking<Unit> {
            val response = mockk<MaanederMedGammelSanksjonIAvkorting>()
            every { avkortingService.harAvkortingMedSanksjonGamleRegler(behandlingId, aar, bruker) } returns response

            klient.harAvkortingMedSanksjonGamleRegler(behandlingId, aar, bruker) shouldBe response
        }

    @Test
    fun `feil fra intern tjeneste sendes videre`() =
        runBlocking<Unit> {
            val feil = IllegalStateException("Beregning feilet")
            coEvery { beregningService.opprettBeregning(behandlingId, bruker) } throws feil

            assertThrows<IllegalStateException> { klient.beregnBehandling(behandlingId, bruker) } shouldBe feil
        }

    @Test
    fun `avviser alle behandlingsoperasjoner uten riktig lese- eller skrivetilgang`() =
        runBlocking<Unit> {
            val saksbehandler = simpleSaksbehandler()
            coEvery { tilgang.harTilgangTilBehandling(behandlingId, any(), saksbehandler) } returns false
            val operasjoner: List<Pair<Boolean, suspend (BrukerTokenInfo) -> Any?>> =
                listOf(
                    false to { klient.hentBeregningOgAvkorting(behandlingId, it) },
                    false to { klient.harOverstyrt(behandlingId, it) },
                    false to { klient.hentOverstyrtBeregning(behandlingId, it) },
                    false to { klient.hentBeregning(behandlingId, it) },
                    false to { klient.hentAvkorting(behandlingId, it) },
                    false to { klient.hentSanksjoner(behandlingId, it) },
                    false to { klient.hentBeregningsgrunnlag(behandlingId, it) },
                    false to { klient.harAvkortingMedSanksjonGamleRegler(behandlingId, aar, it) },
                    false to {
                        klient.hentAvkortingForForbehandlingEtteroppgjoer(
                            EtteroppgjoerBeregnetAvkortingRequest(behandlingId, forrigeBehandlingId, aar, sakId),
                            it,
                        )
                    },
                    false to {
                        klient.hentBeregnetEtteroppgjoerResultat(
                            EtteroppgjoerHentBeregnetResultatRequest(aar, behandlingId, forrigeBehandlingId),
                            it,
                        )
                    },
                    true to { klient.slettAvkorting(behandlingId, it) },
                    true to { klient.beregnBehandling(behandlingId, it) },
                    true to { klient.opprettBeregningsgrunnlagFraForrigeBehandling(behandlingId, forrigeBehandlingId, it) },
                    true to { klient.beregnAvkortingFaktiskInntekt(faktiskInntektRequest(), it) },
                )
            operasjoner.forEach { (_, operasjon) ->
                assertThrows<IkkeTilgangTilBehandlingException> { operasjon(saksbehandler) }
            }
            coVerify(exactly = operasjoner.count { !it.first }) { tilgang.harTilgangTilBehandling(behandlingId, false, saksbehandler) }
            coVerify(exactly = operasjoner.count { it.first }) { tilgang.harTilgangTilBehandling(behandlingId, true, saksbehandler) }
            verify {
                listOf(beregningService, avkortingService, grunnlagService, sanksjonService, etteroppgjoerService, brevService) wasNot
                    Called
            }
        }

    @Test
    fun `saksbehandler med tilgang kan hente beregning`() =
        runBlocking<Unit> {
            val saksbehandler = simpleSaksbehandler()
            val beregning = beregning()
            coEvery { tilgang.harTilgangTilBehandling(behandlingId, false, saksbehandler) } returns true
            every { beregningService.hentBeregning(behandlingId, saksbehandler) } returns beregning

            klient.hentBeregning(behandlingId, saksbehandler) shouldBe beregning.toDTO()
        }

    private fun faktiskInntektRequest() =
        EtteroppgjoerBeregnFaktiskInntektRequest(
            sakId = sakId,
            forbehandlingId = behandlingId,
            sisteIverksatteBehandling = forrigeBehandlingId,
            aar = aar,
            loennsinntekt = 300_000,
            afp = 10_000,
            naeringsinntekt = 20_000,
            utlandsinntekt = 30_000,
            spesifikasjon = "Faktisk inntekt",
            harDoedsfall = true,
            innvilgetPeriodeIEtteroppgjoersAar = Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 9)),
            opphoerFom = YearMonth.of(aar, 10),
            sammenlignTilOgMedBehandlingId = UUID.randomUUID(),
        )
}
