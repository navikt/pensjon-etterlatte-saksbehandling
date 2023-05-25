package avkorting

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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

        service.hentAvkorting(behandlingId) shouldBe avkorting
        verify { avkortingRepository.hentAvkorting(behandlingId) }
    }

    @Test
    fun `skal returnere null hvis avkorting ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        every { avkortingRepository.hentAvkorting(behandlingId) } returns null

        service.hentAvkorting(behandlingId) shouldBe null
        verify { avkortingRepository.hentAvkorting(behandlingId) }
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