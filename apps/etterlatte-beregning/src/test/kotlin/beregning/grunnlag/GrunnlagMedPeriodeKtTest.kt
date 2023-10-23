package beregning.grunnlag

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.erGrunnlagLiktFoerEnDato
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class GrunnlagMedPeriodeKtTest {
    @Test
    fun `erInnenforPeriode gir riktig svar med en lukket periode`() {
        val g1 =
            GrunnlagMedPeriode(
                data = "",
                fom = LocalDate.of(2022, 8, 1),
                tom = YearMonth.of(2023, 6).atEndOfMonth(),
            )

        assertTrue(g1.erInnenforPeriode(periodeFom = YearMonth.of(2022, 8).atDay(1), periodeTom = null))
        assertTrue(g1.erInnenforPeriode(periodeFom = YearMonth.of(2023, 6).atEndOfMonth(), periodeTom = null))

        assertFalse(g1.erInnenforPeriode(periodeFom = g1.tom!!.plusDays(1), null))
        assertFalse(g1.erInnenforPeriode(periodeFom = g1.fom.minusDays(1), g1.fom.minusDays(1)))
    }

    @Test
    fun `erInnenforPeriode gir riktig svar med en åpen periode`() {
        val g1 =
            GrunnlagMedPeriode(
                data = "",
                fom = LocalDate.of(2022, 8, 1),
                tom = null,
            )

        assertTrue(g1.erInnenforPeriode(periodeFom = LocalDate.of(1970, 1, 1), periodeTom = null))
        assertTrue(g1.erInnenforPeriode(periodeFom = LocalDate.of(2050, 1, 1), periodeTom = null))
        assertTrue(g1.erInnenforPeriode(periodeFom = LocalDate.of(2050, 1, 1), periodeTom = LocalDate.of(2050, 1, 1)))

        assertFalse(g1.erInnenforPeriode(periodeFom = g1.fom.minusDays(1), periodeTom = g1.fom.minusDays(1)))
    }

    @Test
    fun `erGrunnlagLiktFoerEnDato gir true hvis grunnlagene er de samme`() {
        val periode = GrunnlagMedPeriode(data = "verdi1", fom = LocalDate.of(2022, 8, 1), tom = null)
        val grunnlag1 = listOf(periode.copy())
        val grunnlag2 = listOf(periode.copy())
        assertTrue(erGrunnlagLiktFoerEnDato(grunnlag1, grunnlag2, LocalDate.of(2023, 1, 1)))
        assertTrue(erGrunnlagLiktFoerEnDato(grunnlag1, grunnlag2, LocalDate.of(2022, 1, 1)))
    }

    @Test
    fun `erGrunnlagLiktFoerEnDato gir true når grunnlaget har annen verdi fra og med cutoff`() {
        val cutoff = LocalDate.of(2023, 1, 1)
        val periode1 = GrunnlagMedPeriode(data = "verdi1", fom = LocalDate.of(2022, 8, 1), tom = null)
        val periode2 = GrunnlagMedPeriode(data = "verdi2", fom = cutoff)

        val grunnlag1 = listOf(periode1)
        val grunnlag2 = listOf(periode1.copy(tom = cutoff.minusDays(1)), periode2)
        assertTrue(erGrunnlagLiktFoerEnDato(grunnlag1, grunnlag2, cutoff))
    }

    @Test
    fun `erGrunnlagLiktFoerEnDato gir false når grunnlaget har annen verdi før cutoff`() {
        val cutoff = LocalDate.of(2023, 1, 1)
        val periode1 = GrunnlagMedPeriode(data = "verdi1", fom = LocalDate.of(2022, 8, 1), tom = null)
        val periode2 = GrunnlagMedPeriode(data = "verdi2", fom = cutoff.minusDays(1))

        val grunnlag1 = listOf(periode1)
        val grunnlag2 = listOf(periode1.copy(tom = cutoff.minusDays(2)), periode2)
        assertFalse(erGrunnlagLiktFoerEnDato(grunnlag1, grunnlag2, cutoff))
    }

    @Test
    fun `erGrunnlagLiktFoerEnDato gir false når perioden før cutoff er forskjellig (men verdien den samme)`() {
        val cutoff = LocalDate.of(2023, 1, 1)
        val periode1 = GrunnlagMedPeriode(data = "verdi1", fom = LocalDate.of(2022, 8, 1), tom = null)
        val periode2 = periode1.copy(fom = periode1.fom.minusMonths(1))

        val grunnlag1 = listOf(periode1)
        val grunnlag2 = listOf(periode2)
        assertFalse(erGrunnlagLiktFoerEnDato(grunnlag1, grunnlag2, cutoff))
    }

    @Test
    fun `erGrunnlagLiktFoerEnDato gir false hvis til og med er endret før dagen før cutoff`() {
        val cutoff = LocalDate.of(2023, 1, 1)
        val periode1 = GrunnlagMedPeriode(data = "verdi1", fom = LocalDate.of(2022, 8, 1), tom = null)
        val periode2 = periode1.copy(tom = cutoff.minusDays(2))

        val grunnlag1 = listOf(periode1)
        val grunnlag2 = listOf(periode2)
        assertFalse(erGrunnlagLiktFoerEnDato(grunnlag1, grunnlag2, cutoff))
    }
}
