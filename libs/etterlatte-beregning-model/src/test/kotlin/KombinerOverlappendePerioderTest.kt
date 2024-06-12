import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.kombinerOverlappendePerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class KombinerOverlappendePerioderTest {
    @Test
    fun `kombinerOverlappendePerioder gir riktige knekkpunkter når en periode er åpen og en er lukket`() {
        val g1 =
            GrunnlagMedPeriode(
                data = "1",
                fom = YearMonth.of(2022, 1).atDay(1),
                tom = null,
            )

        val g2 =
            GrunnlagMedPeriode(
                data = "2",
                fom = YearMonth.of(2022, 8).atDay(1),
                tom = YearMonth.of(2023, 1).atEndOfMonth(),
            )

        val kombinert = listOf(g1, g2).kombinerOverlappendePerioder()
        assertEquals(3, kombinert.size)
        assertEquals(
            setOf(g1.fom, g1.tom, g2.fom, g2.fom, g2.fom.minusDays(1), g2.tom, g2.tom?.plusDays(1)),
            kombinert
                .flatMap {
                    listOf(it.fom, it.tom)
                }.toSet(),
        )
    }

    @Test
    fun `kombinerOverlappendePerioder gir riktige knekkpunkter hvis vi har to lukkede perioder`() {
        val g1 =
            GrunnlagMedPeriode(
                data = "1",
                fom = YearMonth.of(2022, 1).atDay(1),
                tom = YearMonth.of(2023, 7).atEndOfMonth(),
            )

        val g2 =
            GrunnlagMedPeriode(
                data = "2",
                fom = YearMonth.of(2022, 8).atDay(1),
                tom = YearMonth.of(2023, 1).atEndOfMonth(),
            )

        val kombinert = listOf(g1, g2).kombinerOverlappendePerioder()
        val alleStartKnekkpunkt = kombinert.map { it.fom }
        assertEquals(listOf(g1.fom, g2.fom, g2.tom?.plusDays(1)), alleStartKnekkpunkt)

        val alleSluttKnekkpunkt = kombinert.map { it.tom }
        assertEquals(listOf(g2.fom.minusDays(1), g2.tom, g1.tom), alleSluttKnekkpunkt)
    }

    @Test
    fun `kombinerOverlappendePerioder gir riktige knekkpunkter hvis vi har gap i periodene`() {
        val g1 =
            GrunnlagMedPeriode(
                data = "1",
                fom = YearMonth.of(2022, 1).atDay(1),
                tom = YearMonth.of(2022, 5).atEndOfMonth(),
            )

        val g2 =
            GrunnlagMedPeriode(
                data = "2",
                fom = YearMonth.of(2022, 8).atDay(1),
                tom = YearMonth.of(2023, 1).atEndOfMonth(),
            )
        val kombinert = listOf(g1, g2).kombinerOverlappendePerioder()
        assertEquals(3, kombinert.size)

        val alleFom = kombinert.map { it.fom }
        assertEquals(listOf(g1.fom, g1.tom?.plusDays(1), g2.fom), alleFom)

        val alleTom = kombinert.map { it.tom }
        assertEquals(listOf(g1.tom, g2.fom.minusDays(1), g2.tom), alleTom)
    }

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
}
