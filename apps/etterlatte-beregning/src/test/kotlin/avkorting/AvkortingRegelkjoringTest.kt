package avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.avkorting.AvkortingRegelkjoring
import no.nav.etterlatte.beregning.grunnlag.PeriodiseringAvGrunnlagFeil
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.beregningsperiode
import no.nav.etterlatte.libs.common.periode.Periode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class AvkortingRegelkjoringTest {

    @Test
    fun `skal beregne avkorting for inntekt til en foerstegangsbehandling`() {
        val virkningstidspunkt = YearMonth.of(2023, 6)
        val avkortingGrunnlag = listOf(
            avkortinggrunnlag(
                aarsinntekt = 300000,
                fratrekkInnAar = 50000,
                relevanteMaanederInnAar = 10,
                periode = Periode(fom = YearMonth.of(2023, 6), tom = YearMonth.of(2023, 8))
            ),
            avkortinggrunnlag(
                aarsinntekt = 600000,
                fratrekkInnAar = 100000,
                relevanteMaanederInnAar = 10,
                periode = Periode(fom = YearMonth.of(2023, 9), tom = null)
            )
        )

        val avkortingsperioder = AvkortingRegelkjoring.beregnInntektsavkorting(
            Periode(fom = virkningstidspunkt, tom = null),
            avkortingGrunnlag
        )

        with(avkortingsperioder[0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2023, 6)
            periode.tom shouldBe YearMonth.of(2023, 8)
            avkorting shouldBe 9026
        }
        with(avkortingsperioder[1]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2023, 9)
            periode.tom shouldBe null
            avkorting shouldBe 20276
        }
    }

    @Test
    fun `skal ikke tillate aa beregne avkorting for inntekt med feil perioder`() {
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
            AvkortingRegelkjoring.beregnInntektsavkorting(
                Periode(fom = virkningstidspunkt, tom = null),
                avkortingGrunnlag
            )
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

        val avkortetYtelse = AvkortingRegelkjoring.beregnAvkortetYtelse(
            Periode(fom = virkningstidspunkt, tom = null),
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

    // TODO EY-2368 unittest restanse

    // TODO EY-2368 unittest fordelt restanse

}