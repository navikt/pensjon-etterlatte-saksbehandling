package beregning.grunnlag

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodiseringAvGrunnlagFeil
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class PeriodisertBeregningGrunnlagTest {

    private val perioderSomErKomplett = listOf<Pair<LocalDate, LocalDate?>>(
        YearMonth.of(2022, Month.AUGUST).atDay(1) to YearMonth.of(
            2022,
            Month.DECEMBER
        ).atEndOfMonth(),
        YearMonth.of(2023, Month.JANUARY).atDay(1) to null
    )

    private val perioderMedHull = listOf<Pair<LocalDate, LocalDate?>>(
        YearMonth.of(2022, Month.AUGUST).atDay(1) to YearMonth.of(
            2022,
            Month.AUGUST
        ).atEndOfMonth(),
        YearMonth.of(2022, Month.DECEMBER).atDay(1) to YearMonth.of(2022, Month.DECEMBER).atEndOfMonth()
    )

    private val perioderMedOverlapp = listOf<Pair<LocalDate, LocalDate?>>(
        YearMonth.of(2022, Month.AUGUST).atDay(1) to null,
        YearMonth.of(2023, Month.JANUARY).atDay(1) to null
    )

    @Test
    fun `lagKomplettPeriodisertGrunnlag kaster feil hvis ingen perioder er gitt`() {
        val fom = LocalDate.of(2022, 8, 1)
        val tom = null
        assertThrows<PeriodiseringAvGrunnlagFeil.IngenPerioder> {
            PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                listOf<GrunnlagMedPeriode<String>>(),
                fom,
                tom
            )
        }
    }

    @Test
    fun `lagKomplettPeriodisertGrunnlag kaster feil hvis maaneder har hull`() {
        val fom = perioderMedHull.minBy { it.first }.first
        val tom = perioderMedHull.mapNotNull { it.second }.max()
        assertThrows<PeriodiseringAvGrunnlagFeil.PerioderErIkkeKomplett> {
            PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                perioderTilGrunnlagMedPerioder(perioderMedHull, null),
                fom,
                tom
            )
        }
    }

    @Test
    fun `lagKomplettPeriodisertGrunnlag kaster ikke feil hvis grunnlag har hull foer perioden`() {
        val fom = perioderSomErKomplett.minBy { it.first }.first
        val tom = null
        val ekstraPeriodeFoerst =
            YearMonth.from(fom).minusYears(1).atDay(1) to YearMonth.from(fom).minusYears(1).plusMonths(2).atEndOfMonth()
        assertDoesNotThrow {
            PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                perioderTilGrunnlagMedPerioder(
                    listOf(ekstraPeriodeFoerst) + perioderSomErKomplett,
                    null
                ),
                fom,
                tom
            )
        }
    }

    @Test
    fun `lagKomlettPeriodisertGrunnlag kaster ikke feil hvis periodene er komplett`() {
        val fom = perioderSomErKomplett.minBy { it.first }.first
        val tom = null
        assertDoesNotThrow {
            PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                perioderTilGrunnlagMedPerioder(perioderSomErKomplett, null),
                fom,
                tom
            )
        }
    }

    @Test
    fun `lagGrunnlagMedDefault kaster feil hvis ingen perioder er gitt`() {
        assertThrows<PeriodiseringAvGrunnlagFeil.IngenPerioder> {
            PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(emptyList()) { _, _, _ -> "" }
        }
    }

    @Test
    fun `lagGrunnlagMedDefault kaster ikke feil hvis maaneder har hull`() {
        assertDoesNotThrow {
            PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                perioderMedHull.somPeriodegrunnlag()
            ) { _, _, _ -> "" }
        }
    }

    @Test
    fun `lagKomplettPeriodisertGrunnlag kaster feil hvis perioder har overlapp`() {
        val fom = perioderMedOverlapp.minBy { it.first }.first
        val tom = null
        assertThrows<PeriodiseringAvGrunnlagFeil.PerioderOverlapper> {
            PeriodisertBeregningGrunnlag.lagKomplettPeriodisertGrunnlag(
                perioderTilGrunnlagMedPerioder(perioderMedOverlapp, null),
                fom,
                tom
            )
        }
    }

    @Test
    fun `lagGrunnlagMedDefault kaster feil hvis perioder har overlapp`() {
        assertThrows<PeriodiseringAvGrunnlagFeil.PerioderOverlapper> {
            PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                perioderMedOverlapp.somPeriodegrunnlag()
            ) { _, _, _ -> "" }
        }
    }

    @Test
    fun `Grunnlag gir alle forventede knekkpunkter`() {
        val grunnlagHull =
            PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                perioderMedHull.somPeriodegrunnlag()
            ) { _, _, _ -> "" }
        val grunnlagKomplett =
            PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                perioderSomErKomplett.somPeriodegrunnlag()
            ) { _, _, _ -> "" }
        Assertions.assertEquals(
            grunnlagHull.finnAlleKnekkpunkter(),
            setOf(
                LocalDate.of(2022, 8, 1),
                LocalDate.of(2022, 9, 1),
                LocalDate.of(2022, 12, 1),
                LocalDate.of(2023, 1, 1)
            )
        )
        Assertions.assertEquals(
            grunnlagKomplett.finnAlleKnekkpunkter(),
            setOf(LocalDate.of(2022, 8, 1), LocalDate.of(2023, 1, 1))
        )
    }

    private fun List<Pair<LocalDate, LocalDate?>>.somPeriodegrunnlag(): List<GrunnlagMedPeriode<String>> {
        return perioderTilGrunnlagMedPerioder(this, "hei")
    }

    private fun <T> perioderTilGrunnlagMedPerioder(
        perioder: List<Pair<LocalDate, LocalDate?>>,
        opplysning: T
    ): List<GrunnlagMedPeriode<T>> {
        return perioder.map {
            GrunnlagMedPeriode(data = opplysning, fom = it.first, tom = it.second)
        }
    }
}