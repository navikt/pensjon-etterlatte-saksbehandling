package no.nav.etterlatte.avkorting

import io.kotest.matchers.shouldBe
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
import no.nav.etterlatte.beregning.regler.avkortinggrunnlagLagre
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontend
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.sanksjon.SanksjonService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.UUID

internal class AvkortingServiceTest {
    private val behandlingKlient: BehandlingKlient = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()
    private val beregningService: BeregningService = mockk()
    private val sanksjonService: SanksjonService = mockk()
    private val service =
        AvkortingService(
            behandlingKlient,
            avkortingRepository,
            beregningService,
            sanksjonService,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Nested
    inner class HentAvkortingFrontend {
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
                service.hentAvkorting(behandlingId, bruker) shouldBe null
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
            val avkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontend>()

            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            every { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting
            every {
                avkorting.toFrontend(
                    YearMonth.of(2024, 1),
                    null,
                    BehandlingStatus.AVKORTET,
                )
            } returns avkortingFrontend

            runBlocking {
                service.hentAvkorting(behandlingId, bruker) shouldBe avkortingFrontend
            }
            coVerify {
                behandlingKlient.hentBehandling(behandlingId, bruker)
                avkortingRepository.hentAvkorting(behandlingId)
                avkorting.toFrontend(YearMonth.of(2024, 1), null, BehandlingStatus.AVKORTET)
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
                )
            val eksisterendeAvkorting = mockk<Avkorting>()
            val beregning = mockk<Beregning>()
            val reberegnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()
            val avkortinDto = mockk<AvkortingFrontend>()

            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { eksisterendeAvkorting.beregnAvkortingRevurdering(any(), any()) } returns reberegnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            every { lagretAvkorting.toFrontend(any(), any(), any()) } returns avkortinDto

            runBlocking {
                service.hentAvkorting(behandlingId, bruker) shouldBe avkortinDto
            }
            coVerify(exactly = 1) {
                behandlingKlient.avkort(behandlingId, bruker, false)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                eksisterendeAvkorting.beregnAvkortingRevurdering(beregning, any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, reberegnetAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
                lagretAvkorting.toFrontend(YearMonth.of(2024, 1), null, BehandlingStatus.BEREGNET)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(behandlingId)
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
            val eksisterendeAvkorting = mockk<Avkorting>()
            val forrigeAvkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontend>()

            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
                SisteIverksatteBehandling(
                    forrigeBehandlingId,
                )
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { eksisterendeAvkorting.toFrontend(any(), any(), any()) } returns avkortingFrontend

            runBlocking {
                service.hentAvkorting(behandlingId, bruker)
            }

            coVerify(exactly = 1) {
                behandlingKlient.hentBehandling(behandlingId, bruker)
                avkortingRepository.hentAvkorting(behandlingId)
                behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                eksisterendeAvkorting.toFrontend(YearMonth.of(2024, 1), forrigeAvkorting, BehandlingStatus.AVKORTET)
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
            val forrigeAvkorting = mockk<Avkorting>()
            val kopiertAvkorting = mockk<Avkorting>()
            val beregning = mockk<Beregning>()
            val beregnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontend>()

            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
                SisteIverksatteBehandling(
                    forrigeBehandlingId,
                )
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(behandlingId) } returns null andThen lagretAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            every { forrigeAvkorting.kopierAvkorting() } returns kopiertAvkorting
            every { kopiertAvkorting.beregnAvkortingRevurdering(any(), any()) } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            every { lagretAvkorting.toFrontend(any(), any(), any()) } returns avkortingFrontend
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

            runBlocking {
                service.hentAvkorting(behandlingId, bruker)
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(behandlingId, bruker, false)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                forrigeAvkorting.kopierAvkorting()
                kopiertAvkorting.beregnAvkortingRevurdering(beregning, any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)
                lagretAvkorting.toFrontend(YearMonth.of(2024, 1), forrigeAvkorting, BehandlingStatus.BEREGNET)
                behandlingKlient.avkort(behandlingId, bruker, true)
            }
            coVerify(exactly = 2) {
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
            val eksisterendeAvkorting = mockk<Avkorting>()
            val forrigeAvkorting = mockk<Avkorting>()
            val beregning = mockk<Beregning>()
            val reberegnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()
            val avkortingFrontend = mockk<AvkortingFrontend>()

            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
                SisteIverksatteBehandling(
                    forrigeBehandlingId,
                )
            every { avkortingRepository.hentAvkorting(behandlingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            every { eksisterendeAvkorting.beregnAvkortingRevurdering(any(), any()) } returns reberegnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            every { lagretAvkorting.toFrontend(any(), any(), any()) } returns avkortingFrontend

            runBlocking {
                service.hentAvkorting(behandlingId, bruker)
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(behandlingId, bruker, false)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                eksisterendeAvkorting.beregnAvkortingRevurdering(beregning, any())
                avkortingRepository.lagreAvkorting(behandlingId, sakId, reberegnetAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
                lagretAvkorting.toFrontend(YearMonth.of(2024, 1), forrigeAvkorting, BehandlingStatus.BEREGNET)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(behandlingId)
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
        val avkortingFrontend = mockk<AvkortingFrontend>()

        @Test
        fun `Skal beregne og lagre avkorting for førstegangsbehandling`() {
            val behandlingId = UUID.randomUUID()
            val sakId = randomSakId()
            val behandling =
                behandling(
                    id = behandlingId,
                    sak = sakId,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1)),
                )

            every { avkortingRepository.hentAvkorting(any()) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekt(any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(behandlingId) } returns null
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(any(), any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            every { lagretAvkorting.toFrontend(any(), any(), any()) } returns avkortingFrontend

            runBlocking {
                service.beregnAvkortingMedNyttGrunnlag(
                    behandlingId,
                    bruker,
                    endretGrunnlag,
                ) shouldBe avkortingFrontend
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(behandlingId, bruker, false)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                AvkortingValider.validerInntekt(endretGrunnlag, eksisterendeAvkorting, true)
                beregningService.hentBeregningNonnull(behandlingId)
                sanksjonService.hentSanksjon(behandlingId)
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(
                    endretGrunnlag,
                    bruker,
                    beregning,
                    any(),
                    any(),
                )
                avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)
                lagretAvkorting.toFrontend(YearMonth.of(2024, 1), null, BehandlingStatus.BEREGNET)
                behandlingKlient.avkort(behandlingId, bruker, true)
            }
            coVerify(exactly = 2) {
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

            every { avkortingRepository.hentAvkorting(revurderingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns revurdering
            mockkObject(AvkortingValider)
            every { AvkortingValider.validerInntekt(any(), any(), any()) } returns Unit
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { sanksjonService.hentSanksjon(revurderingId) } returns null
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(any(), any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any(), any()) } returns Unit
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
                SisteIverksatteBehandling(
                    forrigeBehandling,
                )
            every { avkortingRepository.hentAvkorting(forrigeBehandling) } returns forrigeAvkorting
            every { lagretAvkorting.toFrontend(any(), any(), any()) } returns avkortingFrontend
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

            runBlocking {
                service.beregnAvkortingMedNyttGrunnlag(
                    revurderingId,
                    bruker,
                    endretGrunnlag,
                ) shouldBe avkortingFrontend
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(revurderingId, bruker, false)
                behandlingKlient.hentBehandling(revurderingId, bruker)
                AvkortingValider.validerInntekt(endretGrunnlag, eksisterendeAvkorting, false)
                beregningService.hentBeregningNonnull(revurderingId)
                sanksjonService.hentSanksjon(revurderingId)
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(
                    endretGrunnlag,
                    bruker,
                    beregning,
                    any(),
                    any(),
                )
                avkortingRepository.lagreAvkorting(revurderingId, sakId, beregnetAvkorting)
                behandlingKlient.hentSisteIverksatteBehandling(sakId, bruker)
                avkortingRepository.hentAvkorting(forrigeBehandling)
                lagretAvkorting.toFrontend(YearMonth.of(2024, 3), forrigeAvkorting, BehandlingStatus.BEREGNET)
                behandlingKlient.avkort(revurderingId, bruker, true)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(revurderingId)
            }
        }

        @Test
        fun `Skal feile ved lagring av avkortinggrunnlag hvis behandling er i feil tilstand`() {
            val behandlingId = UUID.randomUUID()
            coEvery { behandlingKlient.avkort(behandlingId, bruker, false) } returns false

            runBlocking {
                assertThrows<Exception> {
                    service.beregnAvkortingMedNyttGrunnlag(
                        behandlingId,
                        bruker,
                        avkortinggrunnlagLagre(fom = YearMonth.of(2024, 1)),
                    )
                }
            }

            coVerify {
                behandlingKlient.avkort(behandlingId, bruker, false)
            }
        }
    }
}
