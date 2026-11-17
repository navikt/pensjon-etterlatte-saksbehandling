package no.nav.etterlatte.modules.beregning.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakEtteroppgjoerService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerDto
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
import no.nav.etterlatte.modules.beregning.BeregningTestKontekst
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingRepository
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingService
import no.nav.etterlatte.modules.beregning.avkorting.toDto
import no.nav.etterlatte.modules.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.modules.beregning.regler.avkortetYtelse
import no.nav.etterlatte.modules.beregning.regler.avkorting
import no.nav.etterlatte.modules.beregning.regler.bruker
import no.nav.etterlatte.modules.beregning.regler.etteroppgjoer
import no.nav.etterlatte.modules.beregning.regler.ytelseFoerAvkorting
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import no.nav.etterlatte.sak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import java.util.UUID

@ExtendWith(BeregningTestKontekst::class)
internal class EtteroppgjoerServiceTest {
    private val avkortingRepository: AvkortingRepository = mockk()
    private val sanksjonService: SanksjonService = mockk()
    private val etteroppgjoerRepository: EtteroppgjoerRepository = mockk(relaxed = true)
    private val avkortingService: AvkortingService = mockk()
    private val reparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret = mockk()
    private val vedtakEtteroppgjoerService: VedtakEtteroppgjoerService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val forbehandlingService: EtteroppgjoerForbehandlingService = mockk()

    private val service =
        EtteroppgjoerService(
            avkortingRepository,
            sanksjonService,
            etteroppgjoerRepository,
            avkortingService,
            reparerAarsoppgjoeret,
            vedtakEtteroppgjoerService,
            behandlingService,
            featureToggleService,
            forbehandlingService,
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
        every { forbehandlingService.hentForbehandling(any()) } returns
            mockk { every { sak } returns sak(sakId = sakId) }
        every { behandlingService.hentBehandling(any()) } returns foerstegangsbehandling(sakId = sakId)

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

    @ParameterizedTest
    @CsvSource(
        "false,1,1,2",
        "false,1,2,1",
        "false,1,2,2",
        "true,1,1,2",
        "true,1,2,1",
        "true,1,2,2",
    )
    fun `skal avvise ulik sak foer inntekter eller avkorting hentes`(
        beregn: Boolean,
        forbehandlingSak: Long,
        behandlingSak: Long,
        requestSak: Long,
    ) {
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        every { forbehandlingService.hentForbehandling(forbehandlingId) } returns
            mockk { every { sak } returns sak(sakId = SakId(forbehandlingSak)) }
        every { behandlingService.hentBehandling(behandlingId) } returns foerstegangsbehandling(sakId = SakId(behandlingSak))

        assertThrows<EtteroppgjoerBehandlingerTilhoererIkkeSammeSak> {
            if (beregn) {
                service.beregnAvkortingForbehandling(
                    beregnRequest(forbehandlingId, behandlingId).copy(sakId = SakId(requestSak)),
                    bruker,
                )
            } else {
                service.hentBeregnetAvkorting(forbehandlingId, behandlingId, SakId(requestSak), aar, bruker)
            }
        }

        verify(exactly = 0) {
            avkortingService.hentAvkortingMedReparertAarsoppgjoer(any(), any(), any())
            avkortingRepository.hentAvkorting(any())
            avkortingRepository.hentFaktiskInntekt(any())
            avkortingRepository.lagreAvkorting(any(), any(), any())
            sanksjonService.hentSanksjon(any())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `skal avvise resultat for behandlinger fra ulike saker`(beregn: Boolean) {
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        every { forbehandlingService.hentForbehandling(forbehandlingId) } returns
            mockk { every { sak } returns sak(sakId = sakId) }
        every { behandlingService.hentBehandling(behandlingId) } returns foerstegangsbehandling(sakId = SakId(2L))

        assertThrows<EtteroppgjoerBehandlingerTilhoererIkkeSammeSak> {
            if (beregn) {
                service.beregnOgLagreEtteroppgjoerResultat(forbehandlingId, behandlingId, aar, false, null)
            } else {
                service.hentBeregnetEtteroppgjoerResultat(forbehandlingId, behandlingId, aar)
            }
        }

        verify(exactly = 0) {
            vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(any(), any(), any())
            avkortingRepository.hentAvkorting(any())
            avkortingRepository.hentFaktiskInntekt(any())
            etteroppgjoerRepository.hentEtteroppgjoerResultat(any(), any(), any())
            etteroppgjoerRepository.lagreEtteroppgjoerResultat(any())
        }
    }

    @Test
    fun `skal hente avkorting naar alle ID-er tilhoerer samme sak`() {
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        mockForbehandlingMedAvkorting()
        val forventetAvkorting = avkorting(aar = aar)
        every { avkortingService.hentAvkortingMedReparertAarsoppgjoer(sakId, behandlingId, bruker) } returns forventetAvkorting

        val resultat = service.hentBeregnetAvkorting(forbehandlingId, behandlingId, sakId, aar, bruker)

        resultat.avkortingMedForventaInntekt shouldBe forventetAvkorting.toDto()
        verify(exactly = 1) { avkortingRepository.hentAvkorting(forbehandlingId) }
    }

    @Test
    fun `skal beregne og lagre avkorting naar alle ID-er tilhoerer samme sak`() {
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        mockForbehandlingMedAvkorting()
        val tidligereAvkorting =
            Avkorting(
                aarsoppgjoer =
                    listOf(
                        aarsoppgjoer(
                            aar = aar,
                            ytelseFoerAvkorting =
                                listOf(
                                    ytelseFoerAvkorting(
                                        beregning = 20000,
                                        periode = Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 12)),
                                    ),
                                ),
                        ),
                    ),
            )
        every { avkortingService.hentAvkortingMedReparertAarsoppgjoer(sakId, behandlingId, bruker) } returns tidligereAvkorting
        every { sanksjonService.hentSanksjon(behandlingId) } returns emptyList()
        every { featureToggleService.isEnabled(any(), any(), any()) } returns false
        every { avkortingRepository.lagreAvkorting(any(), any(), any()) } just runs

        service.beregnAvkortingForbehandling(beregnRequest(forbehandlingId, behandlingId), bruker)

        verify(exactly = 1) {
            avkortingRepository.lagreAvkorting(
                forbehandlingId,
                sakId,
                withArg { it.aarsoppgjoer.single().aar shouldBe aar },
            )
        }
    }

    private fun beregnRequest(
        forbehandlingId: UUID,
        behandlingId: UUID,
    ) = EtteroppgjoerBeregnFaktiskInntektRequest(
        sakId = sakId,
        forbehandlingId = forbehandlingId,
        sisteIverksatteBehandling = behandlingId,
        aar = aar,
        loennsinntekt = 0,
        afp = 0,
        naeringsinntekt = 0,
        utlandsinntekt = 0,
        spesifikasjon = "Faktisk inntekt",
        harDoedsfall = false,
        innvilgetPeriodeIEtteroppgjoersAar = Periode(YearMonth.of(aar, 1), YearMonth.of(aar, 12)),
        opphoerFom = null,
    )

    @Test
    fun `uten omgjoering hentes hele vedtakslisten og det avgrenses ikke til en behandling`() {
        mockForbehandlingMedAvkorting()
        coEvery { vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(any(), any(), any()) } returns vedtakHeleAaret

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
            vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(sakId, aar, null)
        }
    }

    @Test
    fun `ved klage-omgjoering avgrenses vedtakslisten til og med sammenlign-behandlingen`() {
        mockForbehandlingMedAvkorting()
        val sammenlignTilOgMedBehandlingId = UUID.randomUUID()
        // Behandling-id-en vi avgrenser tidslinjen til – returnerer kun vedtaket som gjaldt før det opprinnelige etteroppgjøret
        coEvery {
            vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(any(), any(), sammenlignTilOgMedBehandlingId)
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
            vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(sakId, aar, sammenlignTilOgMedBehandlingId)
        }
    }
}
