package no.nav.etterlatte.beregning.regler.avkorting.regler

import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerDifferanseGrunnlag
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class EtteroppgjoerBeregningResultatTest {
    private val regelPeriode = RegelPeriode(LocalDate.of(2024, 1, 1))

//    @Test
//    fun `returnere avkortetYtelse for ny brutto stoenad`() {
//        val avkortetYtelse = listOfBruttoStoenad()
//
//        val resultat = nyBruttoStoenad.anvend(grunnlag(listOfUtbetaltStoenad(), avkortetYtelse), regelPeriode)
//        resultat.verdi shouldBe avkortetYtelse
//    }
//
//    @Test
//    fun `returnere avkortetYtelse for utbetalt stoenad`() {
//        val avkortetYtelser = listOfUtbetaltStoenad()
//
//        val resultat = utbetaltStoenad.anvend(grunnlag(avkortetYtelser, listOfBruttoStoenad()), regelPeriode)
//        resultat.verdi shouldBe avkortetYtelser
//    }
//
//    @Test
//    fun `totalt sum av avkortet ytelse for ny brutto stoenad`() {
//        val avkortetYtelser = listOfBruttoStoenad()
//
//        val sumAvkortetYtelse = sumNyBruttoStoenad.anvend(grunnlag(listOfUtbetaltStoenad(), avkortetYtelser), regelPeriode).verdi
//        sumAvkortetYtelse shouldBe 13100
//    }
//
//    @Test
//    fun `totalt sum av avkortet ytelse for utbetalt stoenad`() {
//        val avkortetYtelser = listOfUtbetaltStoenad()
//
//        val sumAvkortetYtelse = sumUtbetaltStoenad.anvend(grunnlag(avkortetYtelser, listOfBruttoStoenad()), regelPeriode).verdi
//        sumAvkortetYtelse shouldBe 12000
//    }
//
//    @Test
//    fun `finne differanse mellom ny brutto stoenad og utbetalt stoenad`() {
//        differanse.anvend(grunnlag(listOfUtbetaltStoenad(), listOfUtbetaltStoenad()), regelPeriode).verdi.differanse shouldBe 0
//        differanse.anvend(grunnlag(listOfUtbetaltStoenad(), listOfBruttoStoenad()), regelPeriode).verdi.differanse shouldBe -1100
//    }

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
