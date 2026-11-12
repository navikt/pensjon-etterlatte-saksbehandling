package no.nav.etterlatte.modules.beregning.avkorting

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.exactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.vedtaksvurdering.InnvilgetPeriode
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtaksvurderingService
import no.nav.etterlatte.behandling.vedtaksvurdering.toVedtakSammendragDto
import no.nav.etterlatte.behandling.vedtaksvurdering.vedtak
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.modules.beregning.Beregning
import no.nav.etterlatte.modules.beregning.BeregningService
import no.nav.etterlatte.modules.beregning.BeregningTestKontekst
import no.nav.etterlatte.modules.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.modules.beregning.regler.avkortinggrunnlagLagreDto
import no.nav.etterlatte.modules.beregning.regler.behandling
import no.nav.etterlatte.modules.beregning.regler.bruker
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.util.UUID

@ExtendWith(BeregningTestKontekst::class)
internal class AvkortingServiceTest {
    private val behandlingStatusService = mockk<BehandlingStatusService>()
    private val behandlingService: BehandlingService = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()
    private val beregningService: BeregningService = mockk()
    private val sanksjonService: SanksjonService = mockk()
    private val aldersovergangService: AldersovergangService = mockk()
    private val vedtaksvurderingService: VedtaksvurderingService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val avkortingReparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret = mockk()
    private val service =
        AvkortingService(
            behandlingService,
            avkortingRepository,
            beregningService,
            sanksjonService,
            aldersovergangService,
            vedtaksvurderingService,
            avkortingReparerAarsoppgjoeret,
            featureToggleService,
            behandlingStatusService = behandlingStatusService,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingStatusService.settAvkortet(any(), any(), any()) } returns Unit
        every { featureToggleService.isEnabled(any(), any(), any()) } returnsArgument 1
        every { sanksjonService.hentSanksjon(any()) } returns emptyList()
        coEvery { vedtaksvurderingService.hentInnvilgedePerioder(any<SakId>()) } returns
            listOf(
                InnvilgetPeriode(
                    Periode(YearMonth.of(1960, 1), null),
                    listOf(vedtak()),
                ),
            )
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
        clearAllMocks()
        unmockkObject(AvkortingMapper, AvkortingValider)
    }

    @Nested
    inner class HentAvkortingFrontendDto {
        @Test
        fun `Foerstegangsbehandling skal returnere null hvis avkorting ikke finnes`() {
            val behandlingId = UUID.randomUUID()
            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                )
            every { avkortingRepository.hentAvkorting(behandlingId) } returns null
            coEvery { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling

            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker) shouldBe null
            }

            coVerify {
                avkortingRepository.hentAvkorting(behandlingId)
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                behandling.behandlingType
            }
        }

        @Test
        fun `Foerstegangsbehandling skal hente avkorting hvis finnes fra foer`() {
            val behandlingId = UUID.randomUUID()
            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.AVKORTET,
                )
            val avkorting = mockk<Avkorting>(relaxed = true)
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val beregning = mockk<Beregning>()

            coEvery { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling
            every { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting
            every { beregningService.hentBeregningNonnull(behandlingId) } returns beregning
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns emptyList()
            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker) shouldBeSameInstanceAs avkortingFrontend
            }
            verify(atLeast = 1) {
                avkorting.aarsoppgjoer
                sanksjonService.hentSanksjon(behandlingId)
            }
            coVerify {
                beregningService.hentBeregningNonnull(behandlingId)
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                avkortingRepository.hentAvkorting(behandlingId)
                AvkortingMapper.avkortingForFrontend(avkorting, behandling)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    avkorting,
                    beregning,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    any(),
                    any(),
                )
                featureToggleService.isEnabled(any(), any(), any())
            }
        }

        @Test
        fun `Foerstegangsbehandling skal reberegne avkorting hvis beregning er beregnet paa nytt`() {
            val behandlingId = UUID.randomUUID()
            val sakId = randomSakId()
            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.BEREGNET,
                    virkningstidspunkt =
                        VirkningstidspunktTestData.virkningstidsunkt(
                            YearMonth.of(
                                Year.now().value,
                                1,
                            ),
                        ),
                )
            val eksisterendeAvkorting = mockk<Avkorting>(relaxed = true)
            val beregning = mockk<Beregning>()
            val reberegnetAvkorting = mockk<Avkorting>(relaxed = true)
            val lagretAvkorting = mockk<Avkorting>(relaxed = true)
            val avkortingFrontend = mockk<AvkortingFrontendDto>()

            coEvery { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every {
                eksisterendeAvkorting.beregnAvkorting(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns reberegnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            every { sanksjonService.hentSanksjon(behandlingId) } returns emptyList()
            coEvery { behandlingStatusService.settAvkortet(any(), any(), any()) } returns Unit
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns emptyList()

            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker) shouldBeSameInstanceAs avkortingFrontend
            }
            coVerify(exactly = 1) {
                behandlingStatusService.settAvkortet(behandlingId, bruker, true)
                eksisterendeAvkorting.beregnAvkorting(any(), beregning, any(), any(), any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, reberegnetAvkorting)
                behandlingStatusService.settAvkortet(behandlingId, bruker, false)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    eksisterendeAvkorting,
                    beregning,
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    any(),
                    any(),
                )
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                avkortingRepository.hentAvkorting(behandlingId)
                beregningService.hentBeregningNonnull(behandlingId)
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                sanksjonService.hentSanksjon(behandlingId)
            }
            verify {
                eksisterendeAvkorting.aarsoppgjoer
            }
        }

        @Test
        fun `Revurdering skal returnere eksisterende avkorting hvis allerede beregnet`() {
            val behandlingId = UUID.randomUUID()
            val behandlingStatusEtterBeregnet = BehandlingStatus.AVKORTET
            val behandling =
                behandling(
                    id = behandlingId,
                    status = behandlingStatusEtterBeregnet,
                    behandlingType = BehandlingType.REVURDERING,
                    sak = randomSakId(),
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1)),
                )
            val forrigeBehandlingId = UUID.randomUUID()
            val eksisterendeAvkorting = mockk<Avkorting>(relaxed = true)
            val beregning =
                mockk<Beregning> {
                    every { beregningsperioder } returns emptyList()
                }
            val forrigeAvkorting = mockk<Avkorting>(relaxed = true)
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandlingId),
                )

            coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
            coEvery { vedtaksvurderingService.hentIverksatteVedtakISak(any()) } returns alleVedtak

            every {
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    any(),
                    any(),
                    any(),
                )
            } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { beregningService.hentBeregningNonnull(behandlingId) } returns beregning
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns emptyList()

            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker)
            }

            verify(atLeast = 1) {
                eksisterendeAvkorting.aarsoppgjoer
                forrigeAvkorting.aarsoppgjoer
            }
            coVerify(exactly = 1) {
                avkortingRepository.hentAvkorting(behandlingId)
                vedtaksvurderingService.hentIverksatteVedtakISak(behandling.sak)
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    any(),
                    any(),
                )
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                beregningService.hentBeregningNonnull(behandlingId)
                AvkortingMapper.avkortingForFrontend(eksisterendeAvkorting, behandling, forrigeAvkorting)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    eksisterendeAvkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                    any(),
                )
                featureToggleService.isEnabled(any(), any(), any())
                vedtaksvurderingService.hentInnvilgedePerioder(behandling.sak)
                sanksjonService.hentSanksjon(behandlingId)
            }

            coVerify(exactly = 2) {
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
            }
        }

        @Test
        fun `Revurdering skal opprette ny avkorting ved aa kopiere tidligere hvis avkorting ikke finnes fra foer`() {
            val behandlingId = UUID.randomUUID()
            val sakId = randomSakId()
            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    behandlingType = BehandlingType.REVURDERING,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1)),
                )
            val forrigeBehandlingId = UUID.randomUUID()
            val forrigeAvkorting =
                mockk<Avkorting> {
                    every { aarsoppgjoer } returns listOf(aarsoppgjoer(2024))
                }
            val kopiertAvkorting = mockk<Avkorting>(relaxed = true)
            val beregning = mockk<Beregning>()
            val beregnetAvkorting = mockk<Avkorting>(relaxed = true)
            val lagretAvkorting = mockk<Avkorting>(relaxed = true)
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandlingId),
                )

            coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
            coEvery { vedtaksvurderingService.hentIverksatteVedtakISak(any()) } returns alleVedtak
            every {
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    any(),
                    any(),
                    any(),
                )
            } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(behandlingId) } returns null andThen lagretAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns emptyList()
            every { forrigeAvkorting.kopierAvkorting() } returns kopiertAvkorting
            every { kopiertAvkorting.beregnAvkorting(any(), any(), any(), any(), any()) } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingStatusService.settAvkortet(any(), any(), any()) } returns Unit
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns emptyList()

            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker)
            }

            coVerify(exactly = 1) {
                behandlingStatusService.settAvkortet(behandlingId, bruker, true)
                vedtaksvurderingService.hentIverksatteVedtakISak(behandling.sak)
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    any(),
                    any(),
                )
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                forrigeAvkorting.kopierAvkorting()
                kopiertAvkorting.beregnAvkorting(any(), beregning, any(), any(), any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)
                behandlingStatusService.settAvkortet(behandlingId, bruker, false)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling, forrigeAvkorting)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    forrigeAvkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                    any(),
                )
                vedtaksvurderingService.hentInnvilgedePerioder(sakId)
            }
            coVerify(exactly = 2) {
                sanksjonService.hentSanksjon(behandlingId)
                featureToggleService.isEnabled(any(), any(), any())
                beregningService.hentBeregningNonnull(behandlingId)
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                avkortingRepository.hentAvkorting(behandlingId)
            }
            verify {
                forrigeAvkorting.aarsoppgjoer
            }
        }

        @Test
        fun `Revurdering skal reberegne avkorting hvis beregning er beregnet paa nytt`() {
            val behandlingId = UUID.randomUUID()
            val sakId = randomSakId()
            val behandlingBeregnetStatus = BehandlingStatus.BEREGNET
            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    status = behandlingBeregnetStatus,
                    behandlingType = BehandlingType.REVURDERING,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1)),
                )
            val forrigeBehandlingId = UUID.randomUUID()
            val eksisterendeAvkorting = mockk<Avkorting>(relaxed = true)
            val forrigeAvkorting = mockk<Avkorting>(relaxed = true)
            val beregning = mockk<Beregning>()
            val reberegnetAvkorting = mockk<Avkorting>(relaxed = true)
            val lagretAvkorting = mockk<Avkorting>(relaxed = true)
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandlingId),
                )

            coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
            coEvery { vedtaksvurderingService.hentIverksatteVedtakISak(any()) } returns alleVedtak
            every {
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    any(),
                    any(),
                    any(),
                )
            } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns emptyList()
            every {
                eksisterendeAvkorting.beregnAvkorting(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns reberegnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingStatusService.settAvkortet(any(), any(), any()) } returns Unit
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns emptyList()

            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker)
            }

            verify(atLeast = 1) {
                eksisterendeAvkorting.aarsoppgjoer
                forrigeAvkorting.aarsoppgjoer
            }
            coVerify(exactly = 1) {
                behandlingStatusService.settAvkortet(behandlingId, bruker, true)
                vedtaksvurderingService.hentIverksatteVedtakISak(behandling.sak)
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    alleVedtak.map { it.toVedtakSammendragDto() },
                    any(),
                )
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                eksisterendeAvkorting.beregnAvkorting(any(), beregning, any(), any(), any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, reberegnetAvkorting)
                behandlingStatusService.settAvkortet(behandlingId, bruker, false)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling, forrigeAvkorting)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    eksisterendeAvkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                    any(),
                )
                vedtaksvurderingService.hentInnvilgedePerioder(sakId)
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                beregningService.hentBeregningNonnull(behandlingId)
                avkortingRepository.hentAvkorting(behandlingId)
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                sanksjonService.hentSanksjon(behandlingId)
            }
            verify {
                eksisterendeAvkorting.aarsoppgjoer
            }
        }
    }

    @Nested
    inner class LagreAvkorting {
        val endretGrunnlag = mockk<AvkortingGrunnlagLagreDto>()
        val beregning = mockk<Beregning>()
        val eksisterendeAvkorting = mockk<Avkorting>(relaxed = true)
        val beregnetAvkorting = mockk<Avkorting>(relaxed = true)
        val lagretAvkorting = mockk<Avkorting>(relaxed = true)
        val avkortingFrontend = mockk<AvkortingFrontendDto>()

        @Test
        fun `Skal beregne og lagre avkorting for førstegangsbehandling`() {
            val behandlingId = UUID.randomUUID()
            val sakId = randomSakId()
            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt =
                        VirkningstidspunktTestData.virkningstidsunkt(
                            YearMonth.of(
                                Year.now().value,
                                1,
                            ),
                        ),
                )

            every { avkortingRepository.hentAvkorting(any()) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekter(any(), any(), any(), any(), any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns emptyList()
            coEvery { aldersovergangService.aldersovergangMaaned(any(), any()) } returns YearMonth.of(1900, 1)
            every { endretGrunnlag.fom } returns YearMonth.of(2024, 1)
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingStatusService.settAvkortet(any(), any(), any()) } returns Unit
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend

            runBlocking {
                service.beregnAvkortingMedNyeGrunnlag(
                    behandlingId,
                    listOf(endretGrunnlag),
                    bruker,
                ) shouldBeSameInstanceAs avkortingFrontend
            }

            coVerify(exactly = 1) {
                behandlingStatusService.settAvkortet(behandlingId, bruker, true)
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                AvkortingValider.validerInntekter(
                    behandling = behandling,
                    beregning = beregning,
                    eksisterendeAvkorting = eksisterendeAvkorting,
                    nyeGrunnlag = listOf(endretGrunnlag),
                    any(),
                    any(),
                    any(),
                )
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                aldersovergangService.aldersovergangMaaned(behandling.sak, SakType.OMSTILLINGSSTOENAD)
                endretGrunnlag.fom
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(
                    listOf(
                        endretGrunnlag,
                    ),
                    bruker,
                    beregning,
                    any(),
                    any(),
                    any(),
                )
                avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)
                behandlingStatusService.settAvkortet(behandlingId, bruker, false)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling)
                vedtaksvurderingService.hentInnvilgedePerioder(sakId)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(behandlingId)
            }
            coVerify(exactly = 3) {
                featureToggleService.isEnabled(any(), any(), any())
            }
        }

        @Test
        fun `Lagre avkorting for revurdering henter og legger til avkorting fra forrige vedtak`() {
            val revurderingId = UUID.randomUUID()
            val sakId = randomSakId()
            val revurdering =
                behandling(
                    id = revurderingId,
                    sak = sakId,
                    behandlingType = BehandlingType.REVURDERING,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 3)),
                )
            val forrigeBehandling = UUID.randomUUID()
            val forrigeAvkorting = mockk<Avkorting>(relaxed = true)
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandling),
                )

            every { avkortingRepository.hentAvkorting(revurderingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns revurdering
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekter(any(), any(), any(), any(), any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(revurderingId) } returns emptyList()
            coEvery { aldersovergangService.aldersovergangMaaned(any(), any()) } returns YearMonth.of(1900, 1)
            every { endretGrunnlag.fom } returns YearMonth.of(2024, 1)
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(any(), any(), any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { vedtaksvurderingService.hentIverksatteVedtakISak(any()) } returns alleVedtak
            every {
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    any(),
                    any(),
                    any(),
                )
            } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandling) } returns forrigeAvkorting
            coEvery { behandlingStatusService.settAvkortet(any(), any(), any()) } returns Unit
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend

            runBlocking {
                service.beregnAvkortingMedNyeGrunnlag(
                    revurderingId,
                    listOf(endretGrunnlag),
                    bruker,
                ) shouldBeSameInstanceAs avkortingFrontend
            }

            verify(atLeast = 1) { forrigeAvkorting.aarsoppgjoer }
            coVerify(exactly = 1) {
                behandlingStatusService.settAvkortet(revurderingId, bruker, true)
                behandlingService.hentDetaljertBehandling(revurderingId, bruker)
                AvkortingValider.validerInntekter(
                    behandling = revurdering,
                    beregning = beregning,
                    eksisterendeAvkorting = eksisterendeAvkorting,
                    nyeGrunnlag = listOf(endretGrunnlag),
                    sanksjoner = emptyList(),
                    krevInntektForNesteAar = true,
                    eksisterendeOpphoerFom = null,
                )
                beregningService.hentBeregningNonnull(revurderingId)
                sanksjonService.hentSanksjon(revurderingId)
                aldersovergangService.aldersovergangMaaned(sakId, SakType.OMSTILLINGSSTOENAD)
                endretGrunnlag.fom
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(
                    listOf(
                        endretGrunnlag,
                    ),
                    bruker,
                    beregning,
                    any(),
                    any(),
                    any(),
                )
                avkortingRepository.lagreAvkorting(revurderingId, sakId, beregnetAvkorting)
                avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    alleVedtak.map { it.toVedtakSammendragDto() },
                    any(),
                )
                avkortingRepository.hentAvkorting(forrigeBehandling)
                behandlingStatusService.settAvkortet(revurderingId, bruker, false)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, revurdering, forrigeAvkorting)
                vedtaksvurderingService.hentIverksatteVedtakISak(sakId)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(revurderingId)
                vedtaksvurderingService.hentInnvilgedePerioder(sakId)
            }
            coVerify(exactly = 3) {
                featureToggleService.isEnabled(any(), any(), any())
            }
        }

        @Test
        fun `Skal feile ved lagring av avkortinggrunnlag hvis behandling er i feil tilstand`() {
            val behandlingId = UUID.randomUUID()
            coEvery { behandlingStatusService.settAvkortet(behandlingId, bruker, true) } throws
                TilstandException.UgyldigTilstand("Ugyldig status")

            runBlocking {
                assertThrows<TilstandException.UgyldigTilstand> {
                    service.beregnAvkortingMedNyeGrunnlag(
                        behandlingId,
                        listOf(avkortinggrunnlagLagreDto(fom = YearMonth.of(2024, 1))),
                        bruker,
                    )
                }
            }

            coVerify {
                behandlingStatusService.settAvkortet(behandlingId, bruker, true)
            }
        }

        @Test
        fun `Skal beregne og lagre avkorting me opphør grunnet aldersovergang`() {
            val behandlingId = UUID.randomUUID()
            val sakId = randomSakId()
            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt =
                        VirkningstidspunktTestData.virkningstidsunkt(
                            YearMonth.of(
                                Year.now().value,
                                1,
                            ),
                        ),
                )
            val foedselsdato67aar = YearMonth.of(2024, 6)

            every { avkortingRepository.hentAvkorting(any()) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingService.hentDetaljertBehandling(any(), any()) } returns behandling
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekter(any(), any(), any(), any(), any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns emptyList()
            coEvery { aldersovergangService.aldersovergangMaaned(any(), any()) } returns foedselsdato67aar
            every { endretGrunnlag.fom } returns YearMonth.of(2024, 1)
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(any(), any(), any(), any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingStatusService.settAvkortet(any(), any(), any()) } returns Unit
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend

            runBlocking {
                service.beregnAvkortingMedNyeGrunnlag(
                    behandlingId,
                    listOf(endretGrunnlag),
                    bruker,
                ) shouldBeSameInstanceAs avkortingFrontend
            }

            coVerify(exactly = 1) {
                behandlingStatusService.settAvkortet(behandlingId, bruker, true)
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                AvkortingValider.validerInntekter(
                    behandling = behandling,
                    beregning = beregning,
                    eksisterendeAvkorting = eksisterendeAvkorting,
                    nyeGrunnlag = listOf(endretGrunnlag),
                    sanksjoner = emptyList(),
                    krevInntektForNesteAar = true,
                    eksisterendeOpphoerFom = null,
                )
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                aldersovergangService.aldersovergangMaaned(behandling.sak, SakType.OMSTILLINGSSTOENAD)
                endretGrunnlag.fom
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(
                    listOf(endretGrunnlag),
                    bruker,
                    beregning,
                    emptyList(),
                    null,
                    any(),
                    foedselsdato67aar,
                )
                avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)
                behandlingStatusService.settAvkortet(behandlingId, bruker, false)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling)
                vedtaksvurderingService.hentInnvilgedePerioder(sakId)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(behandlingId)
            }

            coVerify(exactly = 3) { featureToggleService.isEnabled(any(), any(), any()) }
        }
    }

    @Test
    fun `hvis virk for ny behandling er i et nytt aar maa den vaere i januar`() {
        val behandling = behandling()
        val forrigeBehandlingId = UUID.randomUUID()
        coEvery { vedtaksvurderingService.hentIverksatteVedtakISak(any()) } returns
            listOf(
                vedtak(behandlingId = forrigeBehandlingId, virkningstidspunkt = YearMonth.of(2026, 2)),
            )
        every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns
            Avkorting(listOf(aarsoppgjoer(2025)))

        shouldThrow<NyeAarMedInntektMaaStarteIJanuar> {
            runBlocking {
                service.hentAvkortingForrigeBehandling(
                    behandling = behandling,
                    brukerTokenInfo = bruker,
                    virkningstidspunkt = YearMonth.now(),
                )
            }
        }

        coVerify {
            avkortingRepository.hentAvkorting(forrigeBehandlingId)
            vedtaksvurderingService.hentIverksatteVedtakISak(behandling.sak)
            vedtaksvurderingService.hentInnvilgedePerioder(behandling.sak)
        }
    }

    @Test
    fun `opphørt sak som starter opp igjen midt i et nytt år kaster ikke feil`() {
        val behandling = behandling()
        val forrigeBehandlingId = UUID.randomUUID()
        val forrigeAvkorting = Avkorting(listOf(aarsoppgjoer(2025)))
        val reparertAvkorting = mockk<Avkorting>()

        coEvery { vedtaksvurderingService.hentIverksatteVedtakISak(any()) } returns
            listOf(
                vedtak(behandlingId = forrigeBehandlingId, virkningstidspunkt = YearMonth.of(2025, Month.JANUARY)),
            )
        every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
        // Saken var opphørt etter november 2025 (tom = november) → eksisterendeOpphoerFom = desember 2025
        coEvery { vedtaksvurderingService.hentInnvilgedePerioder(any<SakId>()) } returns
            listOf(
                InnvilgetPeriode(
                    Periode(YearMonth.of(2025, Month.JANUARY), YearMonth.of(2025, Month.NOVEMBER)),
                    listOf(vedtak()),
                ),
            )
        every {
            avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(any(), any(), any())
        } returns reparertAvkorting
        // virk = mars 2026 > desember 2025 (opphoerFom) → skal ikke kaste NyeAarMedInntektMaaStarteIJanuar
        val result =
            runBlocking {
                service.hentAvkortingForrigeBehandling(
                    behandling = behandling,
                    brukerTokenInfo = bruker,
                    virkningstidspunkt = YearMonth.of(2026, Month.MARCH),
                )
            }

        result shouldBeSameInstanceAs reparertAvkorting

        coVerify {
            avkortingRepository.hentAvkorting(forrigeBehandlingId)
            vedtaksvurderingService.hentIverksatteVedtakISak(behandling.sak)
            vedtaksvurderingService.hentInnvilgedePerioder(behandling.sak)
            avkortingReparerAarsoppgjoeret.hentAvkortingMedReparertAarsoppgjoer(
                forrigeAvkorting,
                any(),
                any(),
            )
        }
    }

    @Nested
    inner class HentManglendeInntektsaar {
        @Test
        fun `Returnerer ekstra år når virk er bakover for tidligste fom i aarsoppgjoer`() {
            val behandlingId = UUID.randomUUID()
            val avkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            aarsoppgjoer(aar = 2025, fom = YearMonth.of(2025, 3)),
                        ),
                )
            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2025, 1)),
                )
            val beregning = mockk<Beregning>()

            coEvery { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling
            every { beregningService.hentBeregningNonnull(behandlingId) } returns beregning
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(any(), any(), any(), any(), any(), any(), any())
            } returns emptyList()
            val result =
                runBlocking {
                    service.manglendeInntektsaar(behandlingId, avkorting, bruker)
                }

            result shouldBe listOf(2025)

            coVerify {
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                beregningService.hentBeregningNonnull(behandlingId)
                featureToggleService.isEnabled(any(), any(), any())
                sanksjonService.hentSanksjon(behandlingId)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    avkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

        @Test
        fun `Returnerer ikke ekstra år når tidligste fom er januar`() {
            val behandlingId = UUID.randomUUID()
            val avkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            aarsoppgjoer(aar = 2025, fom = YearMonth.of(2025, 1)),
                        ),
                )
            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2025, 1)),
                )
            val beregning = mockk<Beregning>()

            coEvery { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling
            every { beregningService.hentBeregningNonnull(behandlingId) } returns beregning
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(any(), any(), any(), any(), any(), any(), any())
            } returns emptyList()
            val result =
                runBlocking {
                    service.manglendeInntektsaar(behandlingId, avkorting, bruker)
                }

            result shouldBe emptyList()

            coVerify {
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                beregningService.hentBeregningNonnull(behandlingId)
                featureToggleService.isEnabled(any(), any(), any())
                sanksjonService.hentSanksjon(behandlingId)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    avkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }

        @Test
        fun `Returnerer ikke ekstra år når virk er etter tidligste fom i aarsoppgjoer`() {
            val behandlingId = UUID.randomUUID()
            val avkorting =
                Avkorting(
                    aarsoppgjoer =
                        listOf(
                            aarsoppgjoer(aar = 2025, fom = YearMonth.of(2025, 3)),
                        ),
                )
            val behandling =
                behandling(
                    id = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2025, 5)),
                )
            val beregning = mockk<Beregning>()

            coEvery { behandlingService.hentDetaljertBehandling(behandlingId, bruker) } returns behandling
            every { beregningService.hentBeregningNonnull(behandlingId) } returns beregning
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(any(), any(), any(), any(), any(), any(), any())
            } returns emptyList()
            val result =
                runBlocking {
                    service.manglendeInntektsaar(behandlingId, avkorting, bruker)
                }

            result shouldBe emptyList()

            coVerify {
                behandlingService.hentDetaljertBehandling(behandlingId, bruker)
                beregningService.hentBeregningNonnull(behandlingId)
                featureToggleService.isEnabled(any(), any(), any())
                sanksjonService.hentSanksjon(behandlingId)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    avkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }
    }

    companion object {
        fun vedtakSammendragDto(behandlingId: UUID) = vedtak(behandlingId = behandlingId)
    }
}
