package beregning.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.FNR_1
import no.nav.etterlatte.beregning.regler.FNR_2
import no.nav.etterlatte.beregning.regler.FNR_3
import no.nav.etterlatte.beregning.regler.barnepensjon.beregnBarnepensjon1967Regel
import no.nav.etterlatte.beregning.regler.barnepensjon.beregnBarnepensjon1967RegelMedInstitusjon
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.aktuelleBarnepensjonSatsRegler
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.barnepensjonSatsRegel1967
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.barnepensjonSatsRegel2024
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.toBeregningstall
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class BeregnBarnepensjonTest {
    @BeforeEach
    fun setUp() {
        aktuelleBarnepensjonSatsRegler.clear()
        aktuelleBarnepensjonSatsRegler.add(barnepensjonSatsRegel1967)
        aktuelleBarnepensjonSatsRegler.add(barnepensjonSatsRegel2024)
    }

    @Test
    fun `beregnBarnepensjon1967Regel skal gi 3716,00 ved 40 aars trygdetid og ingen soesken`() {
        val resultat =
            beregnBarnepensjon1967Regel.anvend(
                grunnlag = barnepensjonGrunnlag(),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 3716.00.toBeregningstall()
    }

    @Test
    fun `beregnBarnepensjon1967Regel skal gi 3019,25 ved 40 aars trygdetid og et soesken`() {
        val resultat =
            beregnBarnepensjon1967Regel.anvend(
                grunnlag = barnepensjonGrunnlag(listOf(FNR_1)),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 3019.25.toBeregningstall()
    }

    @Test
    fun `beregnBarnepensjon1967Regel skal gi 2787,00 ved 40 aars trygdetid og to soesken`() {
        val resultat =
            beregnBarnepensjon1967Regel.anvend(
                grunnlag = barnepensjonGrunnlag(listOf(FNR_1, FNR_2)),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 2787.00.toBeregningstall()
    }

    @Test
    fun `beregnBarnepensjon1967Regel gi 2670,875 ved 40 aars trygdetid og tre soesken`() {
        val resultat =
            beregnBarnepensjon1967Regel.anvend(
                grunnlag = barnepensjonGrunnlag(listOf(FNR_1, FNR_2, FNR_3)),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 2670.875.toBeregningstall()
    }

    @Test
    fun `beregnBarnepensjon1967Regel gi 1335,875 ved 20 aars trygdetid og tre soesken`() {
        val resultat =
            beregnBarnepensjon1967Regel.anvend(
                grunnlag =
                    barnepensjonGrunnlag(
                        soeskenKull = listOf(FNR_1, FNR_2, FNR_3),
                        trygdeTid = Beregningstall(20),
                    ),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 1335.4375.toBeregningstall()
    }

    @Test
    fun `kan beregne med institusjonsopphold, 40 aars trygdetid og 3 soesken`() {
        val resultat =
            beregnBarnepensjon1967RegelMedInstitusjon.anvend(
                grunnlag =
                    barnepensjonGrunnlag(
                        soeskenKull = listOf(FNR_1, FNR_2, FNR_3),
                        institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                    ),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 929.toBeregningstall()
    }

    @Test
    fun `kan beregne med institusjonsopphold, 20 aars trygdetid og 3 soesken`() {
        val resultat =
            beregnBarnepensjon1967RegelMedInstitusjon.anvend(
                grunnlag =
                    barnepensjonGrunnlag(
                        soeskenKull = listOf(FNR_1, FNR_2, FNR_3),
                        trygdeTid = Beregningstall(20),
                        institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                    ),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 464.5.toBeregningstall()
    }

    @Test
    fun `skal ikke gi større beløp med institusjonsopphold ingen reduksjon enn vanlig søskenjustering`() {
        val resultatMedInst =
            beregnBarnepensjon1967RegelMedInstitusjon.anvend(
                grunnlag =
                    barnepensjonGrunnlag(
                        soeskenKull = listOf(FNR_1, FNR_2, FNR_3),
                        trygdeTid = Beregningstall(40),
                        institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                    ),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        val resultatUtenInst =
            beregnBarnepensjon1967RegelMedInstitusjon.anvend(
                grunnlag =
                    barnepensjonGrunnlag(
                        soeskenKull = listOf(FNR_1, FNR_2, FNR_3),
                        trygdeTid = Beregningstall(40),
                        institusjonsopphold = null,
                    ),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        Assertions.assertEquals(resultatMedInst.verdi, resultatUtenInst.verdi)
    }

    @Test
    fun `søskenjustering påvirker ikke utbetalt beløp når redusert sats gis (10 % av G)`() {
        val resultatMedSoeskenjustering =
            beregnBarnepensjon1967RegelMedInstitusjon.anvend(
                grunnlag =
                    barnepensjonGrunnlag(
                        soeskenKull = listOf(FNR_1, FNR_2, FNR_3),
                        trygdeTid = Beregningstall(40),
                        institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                    ),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        val resultatUtenSoeskenjustering =
            beregnBarnepensjon1967RegelMedInstitusjon.anvend(
                grunnlag =
                    barnepensjonGrunnlag(
                        soeskenKull = listOf(),
                        trygdeTid = Beregningstall(40),
                        institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                    ),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        Assertions.assertEquals(resultatMedSoeskenjustering.verdi, resultatUtenSoeskenjustering.verdi)
        Assertions.assertEquals(929.0.toBeregningstall(), resultatUtenSoeskenjustering.verdi)
    }

    @Test
    fun `kroneavrundetBarnepensjonRegel skal runde av beloep`() {
        val barnepensjonGrunnlag = barnepensjonGrunnlag(listOf(FNR_1, FNR_2, FNR_3))

        val resultat =
            kroneavrundetBarnepensjonRegel.anvend(
                grunnlag = barnepensjonGrunnlag,
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.verdi shouldBe 2671
    }
}
