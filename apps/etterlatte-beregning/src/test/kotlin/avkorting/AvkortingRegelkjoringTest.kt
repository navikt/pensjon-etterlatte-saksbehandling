package no.nav.etterlatte.avkorting

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class AvkortingRegelkjoringTest {
    @Test
    fun `skal beregne avkorting for inntekt til en foerstegangsbehandling`() {
        val virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, Month.JANUARY))
        val avkortingGrunnlag =
            avkortinggrunnlag(
                periode = Periode(fom = YearMonth.of(2024, Month.JANUARY), tom = null),
            ).copy(
                inntektInnvilgetPeriode =
                    BenyttetInntektInnvilgetPeriode(
                        verdi = 250000,
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                        kilde =
                            Grunnlagsopplysning.RegelKilde(
                                navn = "",
                                ts = Tidspunkt.now(),
                                versjon = "",
                            ),
                    ),
            )

        val avkortingsperioder =
            AvkortingRegelkjoring.beregnInntektsavkorting(
                Periode(fom = virkningstidspunkt.dato, tom = YearMonth.of(2024, Month.DECEMBER)),
                avkortingGrunnlag = avkortingGrunnlag,
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
            periode.tom shouldBe YearMonth.of(2024, Month.DECEMBER)
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

    @Nested
    inner class FaktiskInntektRegel {
        @Test
        fun `Regner med alle inntekter i sum for avkorting`() {
            val periode = RegelPeriode(fraDato = LocalDate.of(2024, 1, 1), tilDato = LocalDate.of(2024, 12, 31))
            val faktiskInntekt =
                AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt(
                    loennsinntekt = 1000,
                    afp = 10000,
                    naeringsinntekt = 100000,
                    utland = 1000000,
                    periode = periode,
                    kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                )

            faktiskInntekt.verdi shouldBe 1111000
        }

        @Test
        fun `regner ingen inntekter som 0`() {
            val periode = RegelPeriode(fraDato = LocalDate.of(2024, 1, 1), tilDato = LocalDate.of(2024, 12, 31))
            val faktiskInntekt =
                AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt(
                    loennsinntekt = 0,
                    afp = 0,
                    naeringsinntekt = 0,
                    utland = 0,
                    periode = periode,
                    kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                )
            faktiskInntekt.verdi shouldBe 0
        }

        @Test
        fun `runder inntekter ned til nærmeste 1000 kr`() {
            val periode = RegelPeriode(fraDato = LocalDate.of(2024, 1, 1), tilDato = LocalDate.of(2024, 12, 31))
            val faktiskInntekt =
                AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt(
                    loennsinntekt = 1999,
                    afp = 0,
                    naeringsinntekt = 0,
                    utland = 0,
                    periode = periode,
                    kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                )
            faktiskInntekt.verdi shouldBe 1000
        }

        @Test
        fun `runder ikke av inntekter som allerede er på nærmeste 1000 kr`() {
            val periode = RegelPeriode(fraDato = LocalDate.of(2024, 1, 1), tilDato = LocalDate.of(2024, 12, 31))
            val faktiskInntekt =
                AvkortingRegelkjoring.beregnInntektInnvilgetPeriodeFaktiskInntekt(
                    loennsinntekt = 2000,
                    afp = 0,
                    naeringsinntekt = 0,
                    utland = 0,
                    periode = periode,
                    kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                )
            faktiskInntekt.verdi shouldBe 2000
        }
    }
}
