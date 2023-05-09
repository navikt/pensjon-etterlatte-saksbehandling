package trygdetid.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.trygdetid.regler.TotalTrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodeGrunnlag
import no.nav.etterlatte.trygdetid.regler.beregnAntallAarTotalTrygdetid
import no.nav.etterlatte.trygdetid.regler.beregnTrygdetidForPeriode
import no.nav.etterlatte.trygdetid.regler.dagerPrMaanedTrygdetid
import no.nav.etterlatte.trygdetid.regler.maksTrygdetid
import no.nav.etterlatte.trygdetid.regler.totalTrygdetidAvrundet
import no.nav.etterlatte.trygdetid.regler.totalTrygdetidFraPerioder
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period

internal class BeregnTrygdetidTest {

    @Test
    fun `beregnTrygdetidMellomToDatoer skal gi en maaned naar periodeFra er dag 1 og periodeTil er siste dag i mnd`() {
        val grunnlag = TrygdetidPeriodeGrunnlag(
            periodeFra = FaktumNode(LocalDate.of(2023, 1, 1), "Z1234", "Periode fra"),
            periodeTil = FaktumNode(LocalDate.of(2023, 1, 31), "Z1234", "Periode til")
        )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.ofMonths(1)
    }

    @Test
    fun `beregnTrygdetidMellomToDatoer skal gi en dag naar periodeFra og periodeTil er like`() {
        val grunnlag = TrygdetidPeriodeGrunnlag(
            periodeFra = FaktumNode(LocalDate.of(2023, 1, 1), "Z1234", "Periode fra"),
            periodeTil = FaktumNode(LocalDate.of(2023, 1, 1), "Z1234", "Periode til")
        )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.ofDays(1)
    }

    @Test
    fun `beregnTrygdetidMellomToDatoer skal gi ett aar en maaned og en dag`() {
        val grunnlag = TrygdetidPeriodeGrunnlag(
            periodeFra = FaktumNode(LocalDate.of(2023, 1, 1), "Z1234", "Periode fra"),
            periodeTil = FaktumNode(LocalDate.of(2024, 2, 1), "Z1234", "Periode til")
        )

        val resultat = beregnTrygdetidForPeriode.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(1, 1, 1)
    }

    @Test
    fun `antallDagerForEnMaanedTrygdetid skal returnere 30`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(1, 0, 0)))

        val resultat = dagerPrMaanedTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 30
    }

    @Test
    fun `totalTrygdetidFraPerioder skal summere perioder `() {
        val grunnlag = totalTrygdetidGrunnlag(
            listOf(
                Period.of(1, 1, 1),
                Period.of(1, 1, 1)
            )
        )

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(2, 2, 2)
    }

    @Test
    fun `totalTrygdetidFraPerioder skal summere perioder og legge til en maaned for hver 30 resterende dager`() {
        val grunnlag = totalTrygdetidGrunnlag(
            listOf(
                Period.ofDays(11),
                Period.ofDays(21),
                Period.ofDays(29)
            )
        )

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(0, 2, 1)
    }

    @Test
    fun `totalTrygdetidFraPerioder skal summere perioder og normalisere overskytende maaneder til aar`() {
        val grunnlag = totalTrygdetidGrunnlag(
            listOf(
                Period.ofMonths(11),
                Period.ofMonths(2)
            )
        )

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(1, 1, 0)
    }

    @Test
    fun `totalTrygdetidFraPerioder skal normalisere overskytende dager til maaned`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.ofDays(31)))

        val resultat = totalTrygdetidFraPerioder.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe Period.of(0, 1, 1)
    }

    @Test
    fun `maksTrygdetid skal returnere 40`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(1, 0, 0)))

        val resultat = maksTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 40
    }

    @Test
    fun `totalTrygdetidAvrundet skal runde opp dersom trygdetid har 6 maaneder eller mer`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(10, 6, 2)))

        val resultat = totalTrygdetidAvrundet.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 11
    }

    @Test
    fun `totalTrygdetidAvrundet skal runde ned dersom trygdetid har 5 maaneder eller mindre`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.of(10, 5, 2)))

        val resultat = totalTrygdetidAvrundet.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 10
    }

    @Test
    fun `beregnAntallAarTrygdetid skal returnere total trygdetid naar denne er mindre enn maks trygdetid`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.ofYears(39)))

        val resultat = beregnAntallAarTotalTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 39
    }

    @Test
    fun `beregnAntallAarTrygdetid skal returnere maks trygdetid naar denne er mindre enn total trygdetid`() {
        val grunnlag = totalTrygdetidGrunnlag(listOf(Period.ofYears(41)))

        val resultat = beregnAntallAarTotalTrygdetid.anvend(grunnlag, RegelPeriode(LocalDate.now()))

        resultat.verdi shouldBe 40
    }

    private fun totalTrygdetidGrunnlag(perioder: List<Period>) = TotalTrygdetidGrunnlag(
        FaktumNode(perioder, Grunnlagsopplysning.Saksbehandler("Z12345", Tidspunkt.now()), "Trygdetidsperioder")
    )
}