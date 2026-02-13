package no.nav.etterlatte.avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagreDto
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Year
import java.time.YearMonth
import java.util.UUID

internal class AvkortingServiceTest {
    private val behandlingKlient: BehandlingKlient = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()
    private val beregningService: BeregningService = mockk()
    private val sanksjonService: SanksjonService = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val vedtaksvurderingKlient: VedtaksvurderingKlient = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val avkortingReparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret = mockk()

    private val service =
        AvkortingService(
            behandlingKlient,
            avkortingRepository,
            beregningService,
            sanksjonService,
            grunnlagKlient,
            vedtaksvurderingKlient,
            avkortingReparerAarsoppgjoeret,
            featureToggleService,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
        every { featureToggleService.isEnabled(any(), any(), any()) } returnsArgument 1
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
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
            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling

            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker) shouldBe null
            }

            coVerify {
                avkortingRepository.hentAvkorting(behandlingId)
                behandlingKlient.hentBehandling(behandlingId, bruker)
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
            val avkorting =
                mockk<Avkorting> {
                    every { aarsoppgjoer } returns emptyList()
                }
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val beregning = mockk<Beregning>()

            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
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
                )
            } returns emptyList()
            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker) shouldBeSameInstanceAs avkortingFrontend
            }
            coVerify {
                beregningService.hentBeregningNonnull(behandlingId)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                avkortingRepository.hentAvkorting(behandlingId)
                AvkortingMapper.avkortingForFrontend(avkorting, behandling)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    avkorting,
                    beregning,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    any(),
                )
                avkorting.aarsoppgjoer
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
            val eksisterendeAvkorting =
                mockk<Avkorting> {
                    every { aarsoppgjoer } returns emptyList()
                }
            val beregning = mockk<Beregning>()
            val reberegnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontendDto>()

            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { eksisterendeAvkorting.beregnAvkorting(any(), any(), any(), any(), any()) } returns reberegnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
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
                behandlingKlient.avkort(behandlingId, bruker, false)
                sanksjonService.hentSanksjon(behandlingId)
                eksisterendeAvkorting.beregnAvkorting(any(), beregning, any(), any(), any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, reberegnetAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    eksisterendeAvkorting,
                    beregning,
                    BehandlingType.FØRSTEGANGSBEHANDLING,
                    any(),
                )
                eksisterendeAvkorting.aarsoppgjoer
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                avkortingRepository.hentAvkorting(behandlingId)
                beregningService.hentBeregningNonnull(behandlingId)
                behandlingKlient.hentBehandling(behandlingId, bruker)
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
            val eksisterendeAvkorting =
                mockk<Avkorting> {
                    every { aarsoppgjoer } returns emptyList()
                }
            val beregning =
                mockk<Beregning> {
                    every { beregningsperioder } returns emptyList()
                }
            val forrigeAvkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandlingId),
                )

            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns alleVedtak

            every {
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
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
                )
            } returns emptyList()

            runBlocking {
                service.hentOpprettEllerReberegnAvkorting(behandlingId, bruker)
            }

            coVerify(exactly = 1) {
                avkortingRepository.hentAvkorting(behandlingId)
                vedtaksvurderingKlient.hentIverksatteVedtak(behandling.sak, bruker)
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    YearMonth.of(2024, 1),
                    alleVedtak,
                )
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                beregningService.hentBeregningNonnull(behandlingId)
                AvkortingMapper.avkortingForFrontend(eksisterendeAvkorting, behandling, forrigeAvkorting)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    eksisterendeAvkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                )
                eksisterendeAvkorting.aarsoppgjoer
                featureToggleService.isEnabled(any(), any(), any())
            }

            coVerify(exactly = 2) {
                behandlingKlient.hentBehandling(behandlingId, bruker)
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
                    every { aarsoppgjoer } returns emptyList()
                }
            val kopiertAvkorting = mockk<Avkorting>()
            val beregning = mockk<Beregning>()
            val beregnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandlingId),
                )

            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns alleVedtak
            every {
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
                    any(),
                    any(),
                    any(),
                )
            } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(behandlingId) } returns null andThen lagretAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            every { forrigeAvkorting.kopierAvkorting() } returns kopiertAvkorting
            every { kopiertAvkorting.beregnAvkorting(any(), any(), any(), any(), any()) } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
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
                behandlingKlient.avkort(behandlingId, bruker, false)
                vedtaksvurderingKlient.hentIverksatteVedtak(behandling.sak, bruker)
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    YearMonth.of(2024, 1),
                    alleVedtak,
                )
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                forrigeAvkorting.kopierAvkorting()
                kopiertAvkorting.beregnAvkorting(any(), beregning, any(), any(), any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling, forrigeAvkorting)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    forrigeAvkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                )
                forrigeAvkorting.aarsoppgjoer
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                beregningService.hentBeregningNonnull(behandlingId)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                avkortingRepository.hentAvkorting(behandlingId)
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
            val eksisterendeAvkorting =
                mockk<Avkorting> {
                    every { aarsoppgjoer } returns emptyList()
                }
            val forrigeAvkorting = mockk<Avkorting>()
            val beregning = mockk<Beregning>()
            val reberegnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontendDto>()
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandlingId),
                )

            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns alleVedtak
            every {
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
                    any(),
                    any(),
                    any(),
                )
            } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            every { eksisterendeAvkorting.beregnAvkorting(any(), any(), any(), any(), any()) } returns reberegnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend
            mockkObject(AvkortingValider)
            every {
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
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
                behandlingKlient.avkort(behandlingId, bruker, false)
                vedtaksvurderingKlient.hentIverksatteVedtak(behandling.sak, bruker)
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    YearMonth.of(2024, 1),
                    alleVedtak,
                )
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                eksisterendeAvkorting.beregnAvkorting(any(), beregning, any(), any(), any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, reberegnetAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling, forrigeAvkorting)
                AvkortingValider.paakrevdeInntekterForBeregningAvAvkorting(
                    eksisterendeAvkorting,
                    beregning,
                    BehandlingType.REVURDERING,
                    any(),
                )
                eksisterendeAvkorting.aarsoppgjoer
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                beregningService.hentBeregningNonnull(behandlingId)
                avkortingRepository.hentAvkorting(behandlingId)
                behandlingKlient.hentBehandling(behandlingId, bruker)
            }
        }
    }

    // TODO hent avkorting ikke frontend..

    @Nested
    inner class LagreAvkorting {
        val endretGrunnlag = mockk<AvkortingGrunnlagLagreDto>()
        val beregning = mockk<Beregning>()

        val eksisterendeAvkorting = mockk<Avkorting>()
        val beregnetAvkorting = mockk<Avkorting>()
        val lagretAvkorting = mockk<Avkorting>()
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
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekter(any(), any(), any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            coEvery { grunnlagKlient.aldersovergangMaaned(any(), any(), any()) } returns YearMonth.of(1900, 1)
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
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
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
                behandlingKlient.avkort(behandlingId, bruker, false)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                AvkortingValider.validerInntekter(
                    behandling = behandling,
                    beregning = beregning,
                    eksisterendeAvkorting = eksisterendeAvkorting,
                    nyeGrunnlag = listOf(endretGrunnlag),
                    any(),
                )
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                grunnlagKlient.aldersovergangMaaned(behandling.sak, SakType.OMSTILLINGSSTOENAD, bruker)
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
                behandlingKlient.avkort(behandlingId, bruker, true)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling)
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                avkortingRepository.hentAvkorting(behandlingId)
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
            val forrigeAvkorting = mockk<Avkorting>()
            val alleVedtak =
                listOf(
                    vedtakSammendragDto(behandlingId = forrigeBehandling),
                )

            every { avkortingRepository.hentAvkorting(revurderingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns revurdering
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekter(any(), any(), any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(revurderingId) } returns null
            coEvery { grunnlagKlient.aldersovergangMaaned(any(), any(), any()) } returns YearMonth.of(1900, 1)
            every { endretGrunnlag.fom } returns YearMonth.of(2024, 1)
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(any(), any(), any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { vedtaksvurderingKlient.hentIverksatteVedtak(any(), any()) } returns alleVedtak
            every {
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
                    any(),
                    any(),
                    any(),
                )
            } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandling) } returns forrigeAvkorting
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            mockkObject(AvkortingMapper)
            every { AvkortingMapper.avkortingForFrontend(any(), any(), any()) } returns avkortingFrontend

            runBlocking {
                service.beregnAvkortingMedNyeGrunnlag(
                    revurderingId,
                    listOf(endretGrunnlag),
                    bruker,
                ) shouldBeSameInstanceAs avkortingFrontend
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(revurderingId, bruker, false)
                behandlingKlient.hentBehandling(revurderingId, bruker)
                AvkortingValider.validerInntekter(
                    behandling = revurdering,
                    beregning = beregning,
                    eksisterendeAvkorting = eksisterendeAvkorting,
                    nyeGrunnlag = listOf(endretGrunnlag),
                    krevInntektForNesteAar = true,
                )
                beregningService.hentBeregningNonnull(revurderingId)
                sanksjonService.hentSanksjon(revurderingId)
                grunnlagKlient.aldersovergangMaaned(sakId, SakType.OMSTILLINGSSTOENAD, bruker)
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
                vedtaksvurderingKlient.hentIverksatteVedtak(sakId, bruker)
                avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
                    forrigeAvkorting,
                    YearMonth.of(2024, 3),
                    alleVedtak,
                )
                avkortingRepository.hentAvkorting(forrigeBehandling)
                behandlingKlient.avkort(revurderingId, bruker, true)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, revurdering, forrigeAvkorting)
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                avkortingRepository.hentAvkorting(revurderingId)
            }
        }

        @Test
        fun `Skal feile ved lagring av avkortinggrunnlag hvis behandling er i feil tilstand`() {
            val behandlingId = UUID.randomUUID()
            coEvery { behandlingKlient.avkort(behandlingId, bruker, false) } returns false

            runBlocking {
                assertThrows<Exception> {
                    service.beregnAvkortingMedNyeGrunnlag(
                        behandlingId,
                        listOf(avkortinggrunnlagLagreDto(fom = YearMonth.of(2024, 1))),
                        bruker,
                    )
                }
            }

            coVerify {
                behandlingKlient.avkort(behandlingId, bruker, false)
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
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekter(any(), any(), any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            coEvery { grunnlagKlient.aldersovergangMaaned(any(), any(), any()) } returns foedselsdato67aar
            every { endretGrunnlag.fom } returns YearMonth.of(2024, 1)
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyeGrunnlag(any(), any(), any(), any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
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
                behandlingKlient.avkort(behandlingId, bruker, false)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                AvkortingValider.validerInntekter(
                    behandling = behandling,
                    beregning = beregning,
                    eksisterendeAvkorting = eksisterendeAvkorting,
                    nyeGrunnlag = listOf(endretGrunnlag),
                    krevInntektForNesteAar = true,
                )
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                grunnlagKlient.aldersovergangMaaned(behandling.sak, SakType.OMSTILLINGSSTOENAD, bruker)
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
                behandlingKlient.avkort(behandlingId, bruker, true)
                AvkortingMapper.avkortingForFrontend(lagretAvkorting, behandling)
            }
            coVerify(exactly = 2) {
                featureToggleService.isEnabled(any(), any(), any())
                avkortingRepository.hentAvkorting(behandlingId)
            }
        }
    }

    companion object {
        fun vedtakSammendragDto(behandlingId: UUID) =
            VedtakSammendragDto(
                id = "id",
                behandlingId = behandlingId,
                vedtakType = null,
                behandlendeSaksbehandler = null,
                datoFattet = null,
                attesterendeSaksbehandler = null,
                datoAttestert = null,
                virkningstidspunkt = null,
                opphoerFraOgMed = null,
            )
    }
}
