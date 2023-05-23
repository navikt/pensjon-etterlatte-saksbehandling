package avkorting.regulering

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.avkorting.regulering.RegulerAvkortingService
import no.nav.etterlatte.beregning.AvkortingGrunnlag
import no.nav.etterlatte.beregning.AvkortingService
import no.nav.etterlatte.beregning.regler.avkorting
import no.nav.etterlatte.beregning.regler.bruker
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

internal class RegulerAvkortingServiceTest {

    private val avkortingService: AvkortingService = mockk()
    private val regulerAvkortingService = RegulerAvkortingService(avkortingService)

    @Test
    fun `reguler avkorting henter forrige grunnlag og beregnerer ny avkorting`() {
        val regulering = randomUUID()
        val forrigeBehandling = randomUUID()
        val forrigeGrunnlag = mockk<AvkortingGrunnlag>()
        every { avkortingService.hentAvkorting(forrigeBehandling) } returns mockk() {
            every { avkortingGrunnlag } returns listOf(forrigeGrunnlag)
        }
        val nyAvkoring = avkorting()
        coEvery { avkortingService.lagreAvkorting(regulering, bruker, any()) } returns nyAvkoring

        val avkorting = runBlocking {
            regulerAvkortingService.regulerAvkorting(regulering, forrigeBehandling, bruker)
        }

        avkorting shouldBe nyAvkoring
        coVerify {
            avkortingService.hentAvkorting(forrigeBehandling)
            avkortingService.lagreAvkorting(regulering, bruker, forrigeGrunnlag)
        }
    }
}