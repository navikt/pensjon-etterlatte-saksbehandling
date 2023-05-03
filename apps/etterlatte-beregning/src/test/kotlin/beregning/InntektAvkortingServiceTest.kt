package beregning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.etterlatte.beregning.InntektAvkortingService
import no.nav.etterlatte.beregning.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.beregning.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class InntektAvkortingServiceTest {

    @BeforeEach
    fun before() {
        mockkObject(GrunnbeloepRepository)
        every { GrunnbeloepRepository.historiskeGrunnbeloep } returns listOf(
            Grunnbeloep(
                dato = YearMonth.of(2022, 5),
                grunnbeloep = 111477,
                grunnbeloepPerMaaned = 9290,
                omregningsfaktor = BigDecimal(1.047726)
            )
        )
    }

    @AfterEach
    fun after() {
        unmockkObject(GrunnbeloepRepository)
    }

    @Test
    fun `skal beregene avkorting for inntekt`() {
        val avkortingGrunnlag = avkortinggrunnlag(aarsinntekt = 500000)

        val beregnedeAvkortingGrunnlag = InntektAvkortingService.beregnInntektsavkorting(avkortingGrunnlag)

        with(beregnedeAvkortingGrunnlag [0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode shouldNotBe null
            avkorting shouldBe 16659
        }
    }
}