package no.nav.etterlatte.beregning.regler.avkorting.regler

import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerDifferanseGrunnlag
import no.nav.etterlatte.avkorting.regler.beregneEtteroppgjoerRegel
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.eksekver
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class EtteroppgjoerBeregningResultatTest {
    private val regelPeriode = RegelPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))

    @Test
    fun `returnere beregnet etteroppgjoer resultat`() {
        val differanseGrunnlag = grunnlag(listOfUtbetaltStoenad(), listOfBruttoStoenad())

        val resultat =
            beregneEtteroppgjoerRegel.eksekver(
                KonstantGrunnlag(differanseGrunnlag),
                regelPeriode,
            )

        // TODO
    }

    private fun grunnlag(
        utbetaltStoenad: List<AvkortetYtelse>,
        nyBruttoStoenad: List<AvkortetYtelse>,
    ): EtteroppgjoerDifferanseGrunnlag {
        val aarsoppgjoer = aarsoppgjoer(aar = 2024, fom = YearMonth.of(2024, Month.JANUARY), avkortetYtelse = utbetaltStoenad)
        val nyttAarsoppgjoer = aarsoppgjoer(aar = 2024, fom = YearMonth.of(2024, Month.JANUARY), avkortetYtelse = nyBruttoStoenad)

        return EtteroppgjoerDifferanseGrunnlag(
            utbetaltStoenad = FaktumNode(aarsoppgjoer, "", ""),
            nyBruttoStoenad = FaktumNode(nyttAarsoppgjoer, "", ""),
        )
    }

    private fun listOfUtbetaltStoenad(): List<AvkortetYtelse> =
        listOf(
            avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 6), ytelse = 1000),
            avkortetYtelse(fom = YearMonth.of(2024, 7), YearMonth.of(2024, 12), ytelse = 1000),
        )

    private fun listOfBruttoStoenad(): List<AvkortetYtelse> =
        listOf(
            avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 4), ytelse = 1000),
            avkortetYtelse(fom = YearMonth.of(2024, 5), YearMonth.of(2024, 9), ytelse = 1100),
            avkortetYtelse(fom = YearMonth.of(2024, 10), YearMonth.of(2024, 12), ytelse = 1200),
        )

    private fun avkortetYtelse(
        fom: YearMonth,
        tom: YearMonth? = null,
        ytelse: Int = 1000,
    ): AvkortetYtelse =
        avkortetYtelse(
            periode = Periode(fom = fom, tom = tom),
            ytelseEtterAvkorting = ytelse,
            ytelseEtterAvkortingFoerRestanse = ytelse,
            restanse = null,
            avkortingsbeloep = 0,
            ytelseFoerAvkorting = 0,
            type = AvkortetYtelseType.ETTEROPPJOER,
            inntektsgrunnlag = null,
        )
}
