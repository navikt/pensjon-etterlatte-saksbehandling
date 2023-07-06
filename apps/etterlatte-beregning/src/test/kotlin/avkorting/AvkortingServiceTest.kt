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
import no.nav.etterlatte.beregning.regler.avkorting
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
        fun `Skal hente avkorting`() {
            val behandlingId = UUID.randomUUID()
            val avkorting = avkorting()
            every { avkortingRepository.hentAvkorting(behandlingId) } returns avkorting

            runBlocking {
                service.hentAvkorting(behandlingId, bruker) shouldBe avkorting
            }
            coVerify {
                avkortingRepository.hentAvkorting(behandlingId)
            }
        }

        @Test
        fun `Skal returnere null hvis avkorting ikke finnes`() {
            val behandlingId = UUID.randomUUID()
            every { avkortingRepository.hentAvkorting(behandlingId) } returns null
            val behandling = behandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
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

            every { avkortingRepository.hentAvkorting(behandlingId) } returns null
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns SisteIverksatteBehandling(
                forrigeBehandlingId
            )
            every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting
            every { forrigeAvkorting.kopierAvkorting(any()) } returns kopiertAvkorting
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every { kopiertAvkorting.beregnAvkorting(any(), any(), any()) } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any()) } returns lagretAvkorting
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

            runBlocking {
                service.hentAvkorting(behandlingId, bruker)
            }

            coVerify {
                avkortingRepository.hentAvkorting(behandlingId)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                avkortingRepository.hentAvkorting(forrigeBehandlingId)
                forrigeAvkorting.kopierAvkorting(behandling.virkningstidspunkt!!.dato)
                beregningService.hentBeregningNonnull(behandlingId)
                kopiertAvkorting.beregnAvkorting(
                    behandling.behandlingType,
                    behandling.virkningstidspunkt!!,
                    beregning
                )
                avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
            }
        }
    }

    @Nested
    inner class LagreAvkorting {

        @Test
        fun `Skal beregne og lagre avkorting`() {
            val behandlingId = UUID.randomUUID()
            val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
            val behandling =
                behandling(behandlingType = BehandlingType.REVURDERING, virkningstidspunkt = virkningstidspunkt)
            val endretGrunnlag = mockk<AvkortingGrunnlag>()
            val beregning = mockk<Beregning>()

            val eksisterendeAvkorting = mockk<Avkorting>()
            val beregnetAvkorting = mockk<Avkorting>()
            val lagretAvkorting = mockk<Avkorting>()

            every { avkortingRepository.hentAvkorting(any()) } returns eksisterendeAvkorting
            coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
            every { beregningService.hentBeregningNonnull(any()) } returns beregning
            every {
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(any(), any(), any(), any())
            } returns beregnetAvkorting
            every { avkortingRepository.lagreAvkorting(any(), any()) } returns lagretAvkorting
            coEvery { behandlingKlient.avkort(any(), any(), any()) } returns true

            runBlocking {
                service.lagreAvkorting(behandlingId, bruker, endretGrunnlag) shouldBe lagretAvkorting
            }

            coVerify(exactly = 1) {
                behandlingKlient.avkort(behandlingId, bruker, false)
                avkortingRepository.hentAvkorting(behandlingId)
                behandlingKlient.hentBehandling(behandlingId, bruker)
                beregningService.hentBeregningNonnull(behandlingId)
                eksisterendeAvkorting.beregnAvkortingMedNyttGrunnlag(
                    endretGrunnlag,
                    behandling.behandlingType,
                    virkningstidspunkt,
                    beregning
                )
                avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
                behandlingKlient.avkort(behandlingId, bruker, true)
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