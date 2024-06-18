package no.nav.etterlatte.avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Test
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class AvkortingRegelkjoringTest {
    @Test
    fun `skal beregne avkorting for inntekt til en foerstegangsbehandling`() {
        val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.JANUARY))
        val avkortingGrunnlag =
            avkortinggrunnlag(
                aarsinntekt = 300000,
                fratrekkInnAar = 50000,
                periode = Periode(fom = YearMonth.of(2024, Month.JANUARY), tom = null),
            )

        val avkortingsperioder =
            AvkortingRegelkjoring.beregnInntektsavkorting(
                Periode(fom = virkningstidspunkt.dato, tom = null),
                avkortingGrunnlag,
                12,
            )

        with(avkortingsperioder[0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2024, Month.JANUARY)
            periode.tom shouldBe YearMonth.of(2024, Month.APRIL)
            avkorting shouldBe 7151
        }
        with(avkortingsperioder[1]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2024, Month.MAY)
            periode.tom shouldBe null
            avkorting shouldBe 7049
        }
    }

    @Test
    fun `skal beregne endelig avkortet ytelse`() {
        val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.JANUARY))
        val beregningsId = UUID.randomUUID()
        val beregninger =
            listOf(
                YtelseFoerAvkorting(
                    beregning = 5000,
                    Periode(
                        fom = YearMonth.of(2024, Month.JANUARY),
                        tom = YearMonth.of(2024, Month.MARCH),
                    ),
                    beregningsreferanse = beregningsId,
                ),
                YtelseFoerAvkorting(
                    beregning = 10000,
                    Periode(
                        fom = YearMonth.of(2024, Month.APRIL),
                        tom = null,
                    ),
                    beregningsreferanse = beregningsId,
                ),
            )
        val avkortingsperioder =
            listOf(
                avkortingsperiode(
                    avkorting = 1000,
                    fom = YearMonth.of(2024, Month.JANUARY),
                    tom = YearMonth.of(2024, Month.JANUARY),
                ),
                avkortingsperiode(
                    avkorting = 2000,
                    fom = YearMonth.of(2024, Month.FEBRUARY),
                    tom = YearMonth.of(2024, Month.FEBRUARY),
                ),
                avkortingsperiode(
                    avkorting = 3000,
                    fom = YearMonth.of(2024, Month.MARCH),
                ),
            )

        val avkortetYtelse =
            AvkortingRegelkjoring.beregnAvkortetYtelse(
                Periode(fom = virkningstidspunkt.dato, tom = null),
                beregninger,
                avkortingsperioder,
                AvkortetYtelseType.FORVENTET_INNTEKT,
                emptyList(),
            )

        avkortetYtelse.size shouldBe 4
        with(avkortetYtelse[0]) {
            regelResultat shouldNotBe null
            tidspunkt shouldNotBe null
            periode.fom shouldBe YearMonth.of(2024, Month.JANUARY)
            periode.tom shouldBe YearMonth.of(2024, Month.JANUARY)
            ytelseEtterAvkorting shouldBe 4000
            avkortingsbeloep shouldBe 1000
            ytelseFoerAvkorting shouldBe 5000
        }
        with(avkortetYtelse[1]) {
            periode.fom shouldBe YearMonth.of(2024, Month.FEBRUARY)
            periode.tom shouldBe YearMonth.of(2024, Month.FEBRUARY)
            ytelseEtterAvkorting shouldBe 3000
            avkortingsbeloep shouldBe 2000
            ytelseFoerAvkorting shouldBe 5000
        }
        with(avkortetYtelse[2]) {
            periode.fom shouldBe YearMonth.of(2024, Month.MARCH)
            periode.tom shouldBe YearMonth.of(2024, Month.MARCH)
            ytelseEtterAvkorting shouldBe 2000
            avkortingsbeloep shouldBe 3000
            ytelseFoerAvkorting shouldBe 5000
        }
        with(avkortetYtelse[3]) {
            periode.fom shouldBe YearMonth.of(2024, Month.APRIL)
            periode.tom shouldBe null
            ytelseEtterAvkorting shouldBe 7000
            avkortingsbeloep shouldBe 3000
            ytelseFoerAvkorting shouldBe 10000
        }
    }
}
