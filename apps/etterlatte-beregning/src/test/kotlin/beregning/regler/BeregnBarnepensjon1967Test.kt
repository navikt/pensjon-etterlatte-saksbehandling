package beregning.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.beregning.regler.FNR_2
import no.nav.etterlatte.beregning.regler.FNR_3
import no.nav.etterlatte.beregning.regler.barnepensjon.beregnBarnepensjon1967Regel
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.toBeregningstall
import no.nav.etterlatte.libs.regler.RegelPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class BeregnBarnepensjon1967Test {

    @Test
    fun `beregnBarnepensjon1967Regel skal gi 3716,00 ved 40 aars trygdetid og ingen soesken`() {
        val resultat = beregnBarnepensjon1967Regel.anvend(
            grunnlag = barnepensjonGrunnlag(),
            periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1))
        )

        resultat.verdi shouldBe 3716.00.toBeregningstall()
    }

    @Test
    fun `beregnBarnepensjon1967Regel skal gi 3019,25 ved 40 aars trygdetid og et soesken`() {
        val resultat = beregnBarnepensjon1967Regel.anvend(
            grunnlag = barnepensjonGrunnlag(listOf(FNR_1)),
            periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1))
        )

        resultat.verdi shouldBe 3019.25.toBeregningstall()
    }

    @Test
    fun `beregnBarnepensjon1967Regel skal gi 2787,00 ved 40 aars trygdetid og to soesken`() {
        val resultat = beregnBarnepensjon1967Regel.anvend(
            grunnlag = barnepensjonGrunnlag(listOf(FNR_1, FNR_2)),
            periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1))
        )

        resultat.verdi shouldBe 2787.00.toBeregningstall()
    }

    @Test
    fun `beregnBarnepensjon1967Regel gi 2671,875 ved 40 aars trygdetid og tre soesken`() {
        val resultat = beregnBarnepensjon1967Regel.anvend(
            grunnlag = barnepensjonGrunnlag(listOf(FNR_1, FNR_2, FNR_3)),
            periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1))
        )

        resultat.verdi shouldBe 2670.875.toBeregningstall()
    }

    @Test
    fun `kroneavrundetBarnepensjonRegel skal runde av beloep`() {
        val barnepensjonGrunnlag = barnepensjonGrunnlag(listOf(FNR_1, FNR_2, FNR_3))

        val resultat = kroneavrundetBarnepensjonRegel.anvend(
            grunnlag = barnepensjonGrunnlag,
            periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1))
        )

        resultat.verdi shouldBe 2671
    }
}