package beregning

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.AvkortingRepository
import no.nav.etterlatte.beregning.AvkortingService
import no.nav.etterlatte.beregning.InntektAvkortingService
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.beregnetAvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.bruker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class AvkortingServiceTest {

    private val repository: AvkortingRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val inntektAvkortingService: InntektAvkortingService = mockk()
    private val service = AvkortingService(repository, behandlingKlient, inntektAvkortingService)

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
        every { repository.hentAvkorting(behandlingId) } returns avkorting

        service.hentAvkorting(behandlingId) shouldBe avkorting
        verify { repository.hentAvkorting(behandlingId) }
    }

    @Test
    fun `skal returnere null hvis avkorting ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        every { repository.hentAvkorting(behandlingId) } returns null

        service.hentAvkorting(behandlingId) shouldBe null
        verify { repository.hentAvkorting(behandlingId) }
    }

    @Test
    fun `skal lagre eller oppdatere avkortinggrunnlag`() {
        val behandlingId = UUID.randomUUID()
        val avkorting = avkorting()
        val avkortinggrunnlag = avkortinggrunnlag()
        val inntektsavkorting = listOf(beregnetAvkortingGrunnlag())
        val grunnlagMedBeregnetAvkorting = avkortinggrunnlag.copy(beregnetAvkorting = inntektsavkorting)

        every { inntektAvkortingService.beregnInntektsavkorting(avkortinggrunnlag) } returns inntektsavkorting
        every {
            repository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, grunnlagMedBeregnetAvkorting)
        } returns avkorting

        val result = runBlocking {
            service.lagreAvkortingGrunnlag(behandlingId, bruker, avkortinggrunnlag)
        }

        result shouldBe avkorting
        coVerify(exactly = 1) {
            behandlingKlient.beregn(behandlingId, bruker, false)
            inntektAvkortingService.beregnInntektsavkorting(avkortinggrunnlag)
            repository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, grunnlagMedBeregnetAvkorting)
        }
    }

    @Test
    fun `skal feile ved lagring av avkortinggrunnlag hvis behandling er i feil tilstand`() {
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingKlient.beregn(behandlingId, bruker, false) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreAvkortingGrunnlag(behandlingId, bruker, avkortinggrunnlag())
            }
        }

        coVerify {
            behandlingKlient.beregn(behandlingId, bruker, false)
        }
    }
}