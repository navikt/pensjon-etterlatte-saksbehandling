package no.nav.etterlatte.beregning.regler.avkorting.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerDifferanseGrunnlag
import no.nav.etterlatte.avkorting.regler.beregneEtteroppgjoerRegel
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class EtteroppgjoerBeregningResultatTest {
    val aar = 2025
    private val regelPeriode = RegelPeriode(LocalDate.of(aar, 1, 1), LocalDate.of(aar, 12, 31))

    @Test
    fun `skal etterbetale hvis differanse er mer en grense for etterbetaling`() {
        val nyBruttoStoenad =
            listOf(
                avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 4), ytelse = 2000),
                avkortetYtelse(fom = YearMonth.of(2024, 5), YearMonth.of(2024, 9), ytelse = 2000),
                avkortetYtelse(fom = YearMonth.of(2024, 10), YearMonth.of(2024, 12), ytelse = 2000),
            )

        val differanseGrunnlag = grunnlag(listOfUtbetaltStoenad(), nyBruttoStoenad)
        val resultat =
            beregneEtteroppgjoerRegel.anvend(
                differanseGrunnlag,
                regelPeriode,
            )

        resultat.verdi.differanse.differanse shouldBe -12000
        resultat.verdi.resultatType.name shouldBe EtteroppgjoerResultatType.ETTERBETALING.name
    }

    @Test
    fun `akkurat utenfor grense for tilbakekreving gir tilbakekreving`() {
        val forventet =
            listOf(
                avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 4), ytelse = 11340),
                avkortetYtelse(fom = YearMonth.of(2024, 5), YearMonth.of(2024, 12), ytelse = 12456),
            )

        val nyBruttoStoenad =
            listOf(
                avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 4), ytelse = 10990),
                avkortetYtelse(fom = YearMonth.of(2024, 5), YearMonth.of(2024, 12), ytelse = 12456),
            )

        val differanseGrunnlag = grunnlag(forventet, nyBruttoStoenad)

        val resultat =
            beregneEtteroppgjoerRegel.anvend(
                differanseGrunnlag,
                regelPeriode,
            )

        resultat.verdi.differanse.differanse shouldBe 1400
        resultat.verdi.resultatType.name shouldBe EtteroppgjoerResultatType.TILBAKEKREVING.name
    }

    @Test
    fun `akkurat utenfor grense for etterbetaling gir etterbetaling`() {
        val forventet =
            listOf(
                avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 4), ytelse = 11340),
                avkortetYtelse(fom = YearMonth.of(2024, 5), YearMonth.of(2024, 12), ytelse = 12456),
            )

        val nyBruttoStoenad =
            listOf(
                avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 4), ytelse = 11440),
                avkortetYtelse(fom = YearMonth.of(2024, 5), YearMonth.of(2024, 12), ytelse = 12456),
            )

        val differanseGrunnlag = grunnlag(forventet, nyBruttoStoenad)

        val resultat =
            beregneEtteroppgjoerRegel.anvend(
                differanseGrunnlag,
                regelPeriode,
            )

        resultat.verdi.differanse.differanse shouldBe -400
        resultat.verdi.resultatType.name shouldBe EtteroppgjoerResultatType.ETTERBETALING.name
    }

    @Test
    fun `skal tilbakekreve hvis differanse er mer en grense for tilbakekreving`() {
        val nyBruttoStoenad =
            listOf(
                avkortetYtelse(fom = YearMonth.of(2024, 1), YearMonth.of(2024, 4), ytelse = 800),
                avkortetYtelse(fom = YearMonth.of(2024, 5), YearMonth.of(2024, 9), ytelse = 800),
                avkortetYtelse(fom = YearMonth.of(2024, 10), YearMonth.of(2024, 12), ytelse = 800),
            )

        val differanseGrunnlag = grunnlag(listOfUtbetaltStoenad(), nyBruttoStoenad)
        val resultat =
            beregneEtteroppgjoerRegel.anvend(
                differanseGrunnlag,
                regelPeriode,
            )

        resultat.verdi.differanse.differanse shouldBe 2400
        resultat.verdi.resultatType.name shouldBe EtteroppgjoerResultatType.TILBAKEKREVING.name
    }

    @Test
    fun `ikke etteroppgjør hvis differanse er null`() {
        val differanseGrunnlag = grunnlag(listOfUtbetaltStoenad(), listOfUtbetaltStoenad())
        val resultat =
            beregneEtteroppgjoerRegel.anvend(
                differanseGrunnlag,
                regelPeriode,
            )

        resultat.verdi.differanse.differanse shouldBe 0
        resultat.verdi.resultatType.name shouldBe EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING.name
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
            type = AvkortetYtelseType.ETTEROPPGJOER,
            inntektsgrunnlag = null,
        )
}
