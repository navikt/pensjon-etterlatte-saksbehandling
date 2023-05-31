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
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.AvkortingService
import no.nav.etterlatte.avkorting.InntektAvkortingService
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.beregning
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class AvkortingServiceTest {

    private val behandlingKlient: BehandlingKlient = mockk()
    private val inntektAvkortingService: InntektAvkortingService = mockk()
    private val avkortingRepository: AvkortingRepository = mockk()
    private val beregningService: BeregningService = mockk()
    private val service =
        AvkortingService(behandlingKlient, inntektAvkortingService, avkortingRepository, beregningService)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.beregn(any(), any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal hente avkorting`() {
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
    fun `skal returnere null hvis avkorting ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        every { avkortingRepository.hentAvkorting(behandlingId) } returns null
        val behandling = mockk<DetaljertBehandling>().apply {
            every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
        }
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
    fun `skal returnere kopi av forrige avkorting hvis avkorting ikke finnes for en revurdering`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 123L
        val behandling = mockk<DetaljertBehandling>().apply {
            every { behandlingType } returns BehandlingType.REVURDERING
            every { sak } returns sakId
        }
        val forrigeBehandlingId = UUID.randomUUID()
        val forrigeBehandling = mockk<DetaljertBehandling>().apply {
            every { id } returns forrigeBehandlingId
        }
        val avkortinggrunnlag = avkortinggrunnlag()
        val forrigeAvkorting = mockk<Avkorting>().apply {
            every { avkortingGrunnlag } returns listOf(avkortinggrunnlag)
        }
        val avkortingsperioder = listOf(avkortingsperiode())
        val beregninger = listOf(beregningsperiode())
        val avkortetYtelse = listOf(avkortetYtelse())

        every { avkortingRepository.hentAvkorting(behandlingId) } returns null
        coEvery { behandlingKlient.hentBehandling(behandlingId, bruker) } returns behandling

        coEvery { behandlingKlient.hentSisteIverksatteBehandling(sakId, bruker) } returns forrigeBehandling
        every { avkortingRepository.hentAvkorting(forrigeBehandlingId) } returns forrigeAvkorting

        coEvery { behandlingKlient.avkort(behandlingId, bruker, false) } returns true
        every { inntektAvkortingService.beregnInntektsavkorting(avkortinggrunnlag) } returns avkortingsperioder
        every { beregningService.hentBeregningNonnull(behandlingId) } returns beregning(beregninger)
        every {
            inntektAvkortingService.beregnAvkortetYtelse(beregninger, avkortingsperioder)
        } returns avkortetYtelse
        every {
            avkortingRepository.lagreEllerOppdaterAvkorting(
                behandlingId,
                listOf(avkortinggrunnlag),
                avkortingsperioder,
                avkortetYtelse
            )
        } returns mockk()
        coEvery { behandlingKlient.avkort(behandlingId, bruker, true) } returns true

        runBlocking {
            service.hentAvkorting(behandlingId, bruker)
        }

        coVerify {
            avkortingRepository.hentAvkorting(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, bruker)
            behandling.behandlingType
            behandling.sak
            behandlingKlient.hentSisteIverksatteBehandling(sakId, bruker)
            forrigeBehandling.id
            avkortingRepository.hentAvkorting(forrigeBehandlingId)
            forrigeAvkorting.avkortingGrunnlag
            behandlingKlient.avkort(behandlingId, bruker, false)
            inntektAvkortingService.beregnInntektsavkorting(avkortinggrunnlag)
            beregningService.hentBeregningNonnull(behandlingId)
            inntektAvkortingService.beregnAvkortetYtelse(beregninger, avkortingsperioder)
            avkortingRepository.lagreEllerOppdaterAvkorting(
                behandlingId,
                listOf(avkortinggrunnlag),
                avkortingsperioder,
                avkortetYtelse
            )
            behandlingKlient.avkort(behandlingId, bruker, true)
        }
    }

    @Test
    fun `skal kjoere regler for avkorting og lagre resultat`() {
        val behandlingId = UUID.randomUUID()
        val avkorting = avkorting()
        val avkortinggrunnlag = avkortinggrunnlag()
        val avkortingsperioder = listOf(avkortingsperiode())
        val beregninger = listOf(beregningsperiode())
        val avkortetYtelse = listOf(avkortetYtelse())

        coEvery { behandlingKlient.avkort(behandlingId, bruker, false) } returns true
        every { inntektAvkortingService.beregnInntektsavkorting(avkortinggrunnlag) } returns avkortingsperioder
        every { beregningService.hentBeregningNonnull(behandlingId) } returns beregning(beregninger)
        every {
            inntektAvkortingService.beregnAvkortetYtelse(beregninger, avkortingsperioder)
        } returns avkortetYtelse
        every {
            avkortingRepository.lagreEllerOppdaterAvkorting(
                behandlingId,
                listOf(avkortinggrunnlag),
                avkortingsperioder,
                avkortetYtelse
            )
        } returns avkorting
        coEvery { behandlingKlient.avkort(behandlingId, bruker, true) } returns true

        val result = runBlocking {
            service.lagreAvkorting(behandlingId, bruker, avkortinggrunnlag)
        }

        result shouldBe avkorting
        coVerify(exactly = 1) {
            behandlingKlient.avkort(behandlingId, bruker, false)
            inntektAvkortingService.beregnInntektsavkorting(avkortinggrunnlag)
            beregningService.hentBeregningNonnull(behandlingId)
            inntektAvkortingService.beregnAvkortetYtelse(beregninger, avkortingsperioder)
            avkortingRepository.lagreEllerOppdaterAvkorting(
                behandlingId,
                listOf(avkortinggrunnlag),
                avkortingsperioder,
                avkortetYtelse
            )
            behandlingKlient.avkort(behandlingId, bruker, true)
        }
    }

    @Test
    fun `skal feile ved lagring av avkortinggrunnlag hvis behandling er i feil tilstand`() {
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