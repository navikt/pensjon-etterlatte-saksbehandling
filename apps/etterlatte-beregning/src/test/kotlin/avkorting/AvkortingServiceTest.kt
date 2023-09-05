package avkorting

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.behandling
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth
import java.util.*

internal class AvkortingServiceTest {

    private val behandlingKlient: BehandlingKlient = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()
    private val beregningService: BeregningService = mockk()
    private val service = AvkortingService(
        behandlingKlient,
        avkortingRepository,
        beregningService
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
    inner class HentAvkorting {

        @Test
        fun `Skal hente avkorting med loepende ytelse`() {
            val behandlingId = UUID.randomUUID()
            val behandling = behandling(
                id = behandlingId,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
            )
            val avkorting = mockk<Avkorting>()

            coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling
            every { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting
            every { avkorting.medYtelseFraOgMedVirkningstidspunkt(YearMonth.of(2023, 1)) } returns avkorting

            runBlocking {
                service.hentAvkorting(behandlingId, bruker) shouldBe avkorting
            }
            coVerify {
                behandlingKlient.hentBehandling(behandlingId, bruker)
                avkortingRepository.hentAvkorting(behandlingId)
                avkorting.medYtelseFraOgMedVirkningstidspunkt(YearMonth.of(2023, 1))
            }
        }

        @Test
        fun `Skal returnere null hvis avkorting ikke finnes`() {
            val behandlingId = UUID.randomUUID()
            val behandling = behandling(
                id = behandlingId,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
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
        fun `Revurdering skal opprette ny avkorting ved aa kopiere tidligere hvis avkorting ikke finnes fra foer`() {
            val behandlingId = UUID.randomUUID()
            val behandling = behandling(
                id = behandlingId,
                behandlingType = BehandlingType.REVURDERING,
                sak = 123L,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
            )
            val forrigeBehandlingId = UUID.randomUUID()
            val forrigeAvkorting = mockk<Avkorting>()
            val kopiertAvkorting = mockk<Avkorting>()
            val beregning = mockk<Beregning>()
            val beregnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()

            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns SisteIverksatteBehandling(
                forrigeBehandlingId
            )
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { avkortingRepository.hentAvkorting(behandlingId) } returns null andThen lagretAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { forrigeAvkorting.kopierAvkorting() } returns kopiertAvkorting
            every { kopiertAvkorting.beregnAvkortingRevurdering(any()) } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any()) } returns Unit
            every { lagretAvkorting.medYtelseFraOgMedVirkningstidspunkt(any(), any()) } returns lagretAvkorting
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

            runBlocking {
                service.hentAvkorting(behandlingId, bruker)
            }

            coVerify(exactly = 1) {
                behandlingKlient.hentBehandling(behandlingId, bruker)
                behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                beregningService.hentBeregningNonnull(behandlingId)
                forrigeAvkorting.kopierAvkorting()
                kopiertAvkorting.beregnAvkortingRevurdering(beregning)
                avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
                lagretAvkorting.medYtelseFraOgMedVirkningstidspunkt(YearMonth.of(2023, 1), forrigeAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(behandlingId)
            }
        }
    }

    @Nested
    inner class LagreAvkorting {

        val endretGrunnlag = mockk<AvkortingGrunnlag>()
        val beregning = mockk<Beregning>()

        val eksisterendeAvkorting = mockk<Avkorting>()
        val beregnetAvkorting = mockk<Avkorting>()
        val lagretAvkorting = mockk<Avkorting>()

        @Test
        fun `Skal beregne og lagre avkorting for førstegangsbehandling`() {
            val behandlingId = UUID.randomUUID()
            val behandling = behandling(
                id = behandlingId,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
            )

            every { avkortingRepository.hentAvkorting(any()) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any()) } returns Unit
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true
            every { lagretAvkorting.medYtelseFraOgMedVirkningstidspunkt(any()) } returns lagretAvkorting

            runBlocking {
                service.lagreAvkorting(behandlingId, bruker, endretGrunnlag) shouldBe lagretAvkorting
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(behandlingId, bruker, false)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                beregningService.hentBeregningNonnull(behandlingId)
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(
                    endretGrunnlag,
                    behandling.virkningstidspunkt!!.dato,
                    behandling.behandlingType,
                    beregning
                )
                avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
                lagretAvkorting.medYtelseFraOgMedVirkningstidspunkt(YearMonth.of(2023, 1))
                behandlingKlient.avkort(behandlingId, bruker, true)
            }
            coVerify(exactly = 2) {
                avkortingRepository.hentAvkorting(behandlingId)
            }
        }

        @Test
        fun `Lagre avkorting for revurdering henter og legger til avkorting fra forrige vedtak`() {
            val revurderingId = UUID.randomUUID()
            val sakId = 123L
            val revurdering = behandling(
                id = revurderingId,
                behandlingType = BehandlingType.REVURDERING,
                sak = sakId,
                virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 3))
            )
            val forrigeBehandling = UUID.randomUUID()
            val forrigeAvkorting = mockk<Avkorting>()

            every { avkortingRepository.hentAvkorting(revurderingId) } returns eksisterendeAvkorting andThen lagretAvkorting
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns revurdering
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any()) } returns Unit
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns SisteIverksatteBehandling(
                forrigeBehandling
            )
            every { avkortingRepository.hentAvkorting(forrigeBehandling) } returns forrigeAvkorting
            every { lagretAvkorting.medYtelseFraOgMedVirkningstidspunkt(any(), any()) } returns lagretAvkorting
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

            runBlocking {
                service.lagreAvkorting(revurderingId, bruker, endretGrunnlag) shouldBe lagretAvkorting
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(revurderingId, bruker, false)
                behandlingKlient.hentBehandling(revurderingId, bruker)
                beregningService.hentBeregningNonnull(revurderingId)
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(
                    endretGrunnlag,
                    revurdering.virkningstidspunkt!!.dato,
                    revurdering.behandlingType,
                    beregning
                )
                avkortingRepository.lagreAvkorting(revurderingId, beregnetAvkorting)
                behandlingKlient.hentSisteIverksatteBehandling(sakId, bruker)
                avkortingRepository.hentAvkorting(forrigeBehandling)
                lagretAvkorting.medYtelseFraOgMedVirkningstidspunkt(YearMonth.of(2023, 3), forrigeAvkorting)
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
                    service.lagreAvkorting(behandlingId, bruker, avkortinggrunnlag())
                }
            }

            coVerify {
                behandlingKlient.avkort(behandlingId, bruker, false)
            }
        }
    }

}