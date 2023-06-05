package avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.etterlatte.avkorting.InntektAvkortingService
import no.nav.etterlatte.beregning.grunnlag.PeriodiseringAvGrunnlagFeil
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        val virkningstidspunkt = YearMonth.of(2023, 1)
        val avkortingGrunnlag = listOf(
            avkortinggrunnlag(
                aarsinntekt = 300000,
                periode = Periode(fom = YearMonth.of(2023, 1), tom = YearMonth.of(2023, 3))
            ),
            avkortinggrunnlag(
                aarsinntekt = 500000,
                periode = Periode(fom = YearMonth.of(2023, 4), tom = null)
            )
        )

        val avkortingsperioder = InntektAvkortingService.beregnInntektsavkorting(virkningstidspunkt, avkortingGrunnlag)

        with(avkortingsperioder[0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2023, 1)
            periode.tom shouldBe YearMonth.of(2023, 3)
            avkorting shouldBe 9160
        }
        with(avkortingsperioder[1]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2023, 4)
            periode.tom shouldBe null
            avkorting shouldBe 16660
        }
    }

    @Test
    fun `skal ikke tillate aa beregene avkorting for inntekt med feil perioder`() {
        val virkningstidspunkt = YearMonth.of(2023, 1)
        val avkortingGrunnlag = listOf(
            avkortinggrunnlag(
                aarsinntekt = 300000,
                periode = Periode(fom = YearMonth.of(2023, 1), tom = null)
            ),
            avkortinggrunnlag(
                aarsinntekt = 500000,
                periode = Periode(fom = YearMonth.of(2023, 4), tom = null)
            )
        )

        assertThrows<PeriodiseringAvGrunnlagFeil> {
            InntektAvkortingService.beregnInntektsavkorting(virkningstidspunkt, avkortingGrunnlag)
        }
    }

    @Test
    fun `skal beregne endelig avkortet ytelse`() {
        val virkningstidspunkt = YearMonth.of(2023, 1)
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
                tom = YearMonth.of(2023, 1)
            ),
            avkortingsperiode(
                avkorting = 2000,
                fom = YearMonth.of(2023, 2),
                tom = YearMonth.of(2023, 2)
            ),
            avkortingsperiode(
                avkorting = 3000,
                fom = YearMonth.of(2023, 3)
            )
        )

        val avkortetYtelse = InntektAvkortingService.beregnAvkortetYtelse(
            virkningstidspunkt,
            beregninger,
            avkortingsperioder
        )

        avkortetYtelse.size shouldBe 4
        with(avkortetYtelse[0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2023, 1)
            periode.tom shouldBe YearMonth.of(2023, 1)
            ytelseEtterAvkorting shouldBe 4000
            avkortingsbeloep shouldBe 1000
            ytelseFoerAvkorting shouldBe 5000
        }
        with(avkortetYtelse[1]) {
            periode.fom shouldBe YearMonth.of(2023, 2)
            periode.tom shouldBe YearMonth.of(2023, 2)
            ytelseEtterAvkorting shouldBe 3000
            avkortingsbeloep shouldBe 2000
            ytelseFoerAvkorting shouldBe 5000
        }
        with(avkortetYtelse[2]) {
            periode.fom shouldBe YearMonth.of(2023, 3)
            periode.tom shouldBe YearMonth.of(2023, 3)
            ytelseEtterAvkorting shouldBe 2000
            avkortingsbeloep shouldBe 3000
            ytelseFoerAvkorting shouldBe 5000
        }
        with(avkortetYtelse[3]) {
            periode.fom shouldBe YearMonth.of(2023, 4)
            periode.tom shouldBe null
            ytelseEtterAvkorting shouldBe 7000
            avkortingsbeloep shouldBe 3000
            ytelseFoerAvkorting shouldBe 10000
        }
    }
}