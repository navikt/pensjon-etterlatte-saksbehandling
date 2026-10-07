package no.nav.etterlatte.avkorting.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.Etteroppgjoer
import no.nav.etterlatte.beregning.BeregningToggles
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.etteroppgjoer
import no.nav.etterlatte.beregning.regler.ytelseFoerAvkorting
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerDto
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import java.util.UUID

internal class EtteroppgjoerServiceTest {
    private val avkortingRepository: AvkortingRepository = mockk()
    private val sanksjonService: SanksjonService = mockk()
    private val etteroppgjoerRepository: EtteroppgjoerRepository = mockk(relaxed = true)
    private val avkortingService: AvkortingService = mockk()
    private val reparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret = mockk()
    private val vedtakKlient: VedtaksvurderingKlient = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val service =
        EtteroppgjoerService(
            avkortingRepository,
            sanksjonService,
            etteroppgjoerRepository,
            avkortingService,
            reparerAarsoppgjoeret,
            vedtakKlient,
            behandlingKlient,
            featureToggleService,
        )

    private val aar = 2024
    private val sakId = SakId(1L)

    // To vedtak i etteroppgjørsåret, hvert dekker et halvår med 1000 kr/mnd = 6000 kr hver
    private val vedtakHeleAaret =
        listOf(
            VedtakEtteroppgjoerDto(
                vedtakId = 100L,
                perioder =
                    listOf(
                        VedtakEtteroppgjoerPeriode(YearMonth.of(aar, 1), YearMonth.of(aar, 6), ytelseEtterAvkorting = 1000),
                    ),
            ),
            VedtakEtteroppgjoerDto(
                vedtakId = 200L,
                perioder =
                    listOf(
                        VedtakEtteroppgjoerPeriode(YearMonth.of(aar, 7), YearMonth.of(aar, 12), ytelseEtterAvkorting = 1000),
                    ),
            ),
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    private fun mockForbehandlingMedAvkorting() {
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling(sak = sakId)

        val forbehandlingAvkorting =
            etteroppgjoer(
                aar = aar,
                avkortetYtelse =
                    listOf(
                        avkortetYtelse(
                            periode = Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 12)),
                            ytelseEtterAvkorting = 1000,
                        ),
                    ),
            )
        every { avkortingRepository.hentAvkorting(any()) } returns Avkorting(aarsoppgjoer = listOf(forbehandlingAvkorting))
        every { avkortingRepository.hentFaktiskInntekt(forbehandlingAvkorting.id) } returns forbehandlingAvkorting.inntekt
    }

    @ParameterizedTest(name = "nye regler: {0}, perioder i request: {1}, flerperiode-toggle: {2}")
    @CsvSource(
        "true, true, true",
        "false, true, true",
        "true, false, true",
        "false, false, true",
        "true, true, false",
        "false, true, false",
        "true, false, false",
        "false, false, false",
    )
    fun `etteroppgjoer med hull krever toggle og bevarer ni maaneder og null ytelse`(
        brukNyeRegler: Boolean,
        sendPerioder: Boolean,
        toggleAktiv: Boolean,
    ) {
        val opprinneligRequest = faktiskInntektRequest()
        val innvilgedePerioder = requireNotNull(opprinneligRequest.innvilgedePerioderIEtteroppgjoersAar)
        val request =
            opprinneligRequest.copy(
                innvilgedePerioderIEtteroppgjoersAar = innvilgedePerioder.takeIf { sendPerioder },
            )
        val lagretAvkorting = slot<Avkorting>()
        every { sanksjonService.hentSanksjon(any()) } returns emptyList()
        every { featureToggleService.isEnabled(any(), any(), any()) } returns brukNyeRegler
        every { featureToggleService.isEnabled(BeregningToggles.BEREGN_OVER_FLERE_PERIODER, false) } returns toggleAktiv
        coEvery { avkortingService.hentAvkortingMedReparertAarsoppgjoer(any(), any(), any()) } returns
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            aar = aar,
                            ytelseFoerAvkorting =
                                innvilgedePerioder.map {
                                    ytelseFoerAvkorting(periode = it, beregning = 100)
                                },
                        ),
                    ),
            )
        every { avkortingRepository.lagreAvkorting(request.forbehandlingId, sakId, capture(lagretAvkorting)) } returns Unit

        if (toggleAktiv) {
            service.beregnAvkortingForbehandling(request, bruker)

            val oppgjoer =
                lagretAvkorting.captured.aarsoppgjoer
                    .single()
                    .shouldBeInstanceOf<Etteroppgjoer>()
            oppgjoer.innvilgaMaaneder() shouldBe 9
            oppgjoer.avkortetYtelse.map { it.periode } shouldBe innvilgedePerioder
            oppgjoer.avkortetYtelse.all { it.ytelseEtterAvkorting == 0 } shouldBe true
        } else {
            val feil =
                assertThrows<UgyldigForespoerselException> {
                    service.beregnAvkortingForbehandling(request, bruker)
                }
            feil.code shouldBe "FLERPERIODEBEREGNING_IKKE_AKTIV"
            verify(exactly = 0) { avkortingRepository.lagreAvkorting(any(), any(), any()) }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `sammenhengende etteroppgjoer med flere G-perioder fungerer med toggle av`(sendPerioder: Boolean) {
        val opprinneligRequest = faktiskInntektRequest()
        val request =
            opprinneligRequest.copy(
                innvilgedePerioderIEtteroppgjoersAar =
                    listOf(opprinneligRequest.innvilgetPeriodeIEtteroppgjoersAar).takeIf { sendPerioder },
            )
        val lagretAvkorting = slot<Avkorting>()
        every { sanksjonService.hentSanksjon(any()) } returns emptyList()
        every { featureToggleService.isEnabled(any(), any(), any()) } returns false
        coEvery { avkortingService.hentAvkortingMedReparertAarsoppgjoer(any(), any(), any()) } returns
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            aar = aar,
                            ytelseFoerAvkorting =
                                listOf(
                                    ytelseFoerAvkorting(
                                        periode = Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 4)),
                                        beregning = 100,
                                    ),
                                    ytelseFoerAvkorting(
                                        periode = Periode(YearMonth.of(aar, 5), YearMonth.of(aar, 12)),
                                        beregning = 100,
                                    ),
                                ),
                        ),
                    ),
            )
        every { avkortingRepository.lagreAvkorting(request.forbehandlingId, sakId, capture(lagretAvkorting)) } returns Unit

        service.beregnAvkortingForbehandling(request, bruker)

        lagretAvkorting.captured.aarsoppgjoer
            .single()
            .innvilgaMaaneder() shouldBe 12
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `etteroppgjoer blokkeres naar beregningsgrunnlaget fyller hullet eller mangler en periode`(fyllerHullet: Boolean) {
        val request = faktiskInntektRequest()
        val periode =
            if (fyllerHullet) {
                request.innvilgetPeriodeIEtteroppgjoersAar
            } else {
                Periode(YearMonth.of(aar, 7), YearMonth.of(aar, 12))
            }
        every { sanksjonService.hentSanksjon(any()) } returns emptyList()
        coEvery { avkortingService.hentAvkortingMedReparertAarsoppgjoer(any(), any(), any()) } returns
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(aar = aar, ytelseFoerAvkorting = listOf(ytelseFoerAvkorting(periode = periode))),
                    ),
            )

        assertThrows<UgyldigForespoerselException> {
            service.beregnAvkortingForbehandling(request, bruker)
        }

        verify(exactly = 0) { avkortingRepository.lagreAvkorting(any(), any(), any()) }
    }

    private fun faktiskInntektRequest() =
        EtteroppgjoerBeregnFaktiskInntektRequest(
            sakId = sakId,
            forbehandlingId = UUID.randomUUID(),
            sisteIverksatteBehandling = UUID.randomUUID(),
            aar = aar,
            loennsinntekt = 1_000_000,
            afp = 0,
            naeringsinntekt = 0,
            utlandsinntekt = 0,
            spesifikasjon = "Inntekt i innvilgede perioder",
            harDoedsfall = false,
            innvilgetPeriodeIEtteroppgjoersAar = Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 12)),
            opphoerFom = null,
            innvilgedePerioderIEtteroppgjoersAar =
                listOf(
                    Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 3)),
                    Periode(YearMonth.of(aar, 7), YearMonth.of(aar, 12)),
                ),
        )

    @Test
    fun `uten omgjoering hentes hele vedtakslisten og det avgrenses ikke til en behandling`() {
        mockForbehandlingMedAvkorting()
        coEvery { vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(any(), any(), any(), any()) } returns vedtakHeleAaret

        val resultat =
            runBlocking {
                service.beregnOgLagreEtteroppgjoerResultat(
                    forbehandlingId = UUID.randomUUID(),
                    sisteIverksatteBehandlingId = UUID.randomUUID(),
                    etteroppgjoersAar = aar,
                    harDoedsfall = false,
                    sammenlignTilOgMedBehandlingId = null,
                )
            }

        // Begge vedtakene teller med: 6000 + 6000
        resultat.utbetaltStoenad shouldBe 12000
        resultat.referanseAvkorting.vedtakReferanse shouldBe listOf(100L, 200L)
        coVerify {
            vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(sakId, aar, any(), null)
        }
    }

    @Test
    fun `ved klage-omgjoering avgrenses vedtakslisten til og med sammenlign-behandlingen`() {
        mockForbehandlingMedAvkorting()
        val sammenlignTilOgMedBehandlingId = UUID.randomUUID()
        // Behandling-id-en vi avgrenser tidslinjen til – returnerer kun vedtaket som gjaldt før det opprinnelige etteroppgjøret
        coEvery {
            vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(any(), any(), any(), sammenlignTilOgMedBehandlingId)
        } returns vedtakHeleAaret.take(1)

        val resultat =
            runBlocking {
                service.beregnOgLagreEtteroppgjoerResultat(
                    forbehandlingId = UUID.randomUUID(),
                    sisteIverksatteBehandlingId = UUID.randomUUID(),
                    etteroppgjoersAar = aar,
                    harDoedsfall = false,
                    sammenlignTilOgMedBehandlingId = sammenlignTilOgMedBehandlingId,
                )
            }

        // Kun vedtak 100 teller med: 6000
        resultat.utbetaltStoenad shouldBe 6000
        resultat.referanseAvkorting.vedtakReferanse shouldBe listOf(100L)
        coVerify {
            vedtakKlient.hentVedtakslisteIEtteroppgjoersAar(sakId, aar, any(), sammenlignTilOgMedBehandlingId)
        }
    }
}
