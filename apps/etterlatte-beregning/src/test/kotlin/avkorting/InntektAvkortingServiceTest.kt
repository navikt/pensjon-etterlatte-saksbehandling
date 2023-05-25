package avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.etterlatte.avkorting.InntektAvkortingService
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
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
    fun `skal beregene avkorting for inntekt til en foerstegangsbehandling`() {
        val avkortingGrunnlag = avkortinggrunnlag(aarsinntekt = 300000)

        val beregnedeAvkortingGrunnlag = InntektAvkortingService.beregnInntektsavkorting(avkortingGrunnlag)

        with(beregnedeAvkortingGrunnlag[0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode shouldNotBe null
            avkorting shouldBe 9160
        }
    }

    @Test
    fun `skal beregne endelig avkortet ytelse`() {
        val beregninger = listOf(
            beregningsperiode(
                utbetaltBeloep = 5000,
                datoFOM = YearMonth.of(2023, 1),
                datoTOM = YearMonth.of(2023, 3)
            ),
            beregningsperiode(
                utbetaltBeloep = 10000,
                datoFOM = YearMonth.of(2023, 4)
            )
        )
        val avkortingsperioder = listOf(
            avkortingsperiode(
                avkorting = 1000,
                fom = YearMonth.of(2023, 1),
                tom = YearMonth.of(2023, 2)
            ),
            avkortingsperiode(
                avkorting = 2000,
                fom = YearMonth.of(2023, 2)
            ),
            avkortingsperiode(
                avkorting = 3000,
                fom = YearMonth.of(2023, 3)
            )
        )

        val avkortetYtelse = InntektAvkortingService.beregnAvkortetYtelse(beregninger, avkortingsperioder)

        avkortetYtelse.size shouldBe 4
        with(avkortetYtelse[0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2023, 1)
            periode.tom shouldBe YearMonth.of(2023, 1)
            ytelseEtterAvkorting shouldBe 4000
        }
        with(avkortetYtelse[1]) {
            periode.fom shouldBe YearMonth.of(2023, 2)
            periode.tom shouldBe YearMonth.of(2023, 2)
            ytelseEtterAvkorting shouldBe 3000
        }
        with(avkortetYtelse[2]) {
            periode.fom shouldBe YearMonth.of(2023, 3)
            periode.tom shouldBe YearMonth.of(2023, 3)
            ytelseEtterAvkorting shouldBe 2000
        }
        with(avkortetYtelse[3]) {
            periode.fom shouldBe YearMonth.of(2023, 4)
            periode.tom shouldBe null
            ytelseEtterAvkorting shouldBe 7000
        }
    }
}