package no.nav.etterlatte.beregning.regler.beregning.regler

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.MAKS_TRYGDETID
import no.nav.etterlatte.beregning.regler.barnepensjon.PeriodisertBarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.kroneavrundetBarnepensjonRegelMedInstitusjon
import no.nav.etterlatte.beregning.regler.finnAnvendtRegelverkBarnepensjon
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.PeriodisertResultat
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class VisitorsTest {
    @Test
    fun `finnAnvendtRegelverkBarnepensjon skal returnere BP_REGELVERK_TOM_2023 tom 2023 og BP_REGELVERK_FOM_2024 fom 2024`() {
        val periodiserteResultater =
            kroneavrundetBarnepensjonRegelMedInstitusjon
                .eksekver(
                    grunnlag = opprettBeregningsgrunnlag(),
                    periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1), tilDato = LocalDate.of(2024, Month.JULY, 31)),
                ).let {
                    if (it is RegelkjoeringResultat.Suksess) it.periodiserteResultater else fail("Regelkjøring feilet")
                }

        // Perioder tom 2023 skal returnere BP_REGELVERK_TOM_2023

        val periodeFraJan2023 = periodiserteResultater.fraDato(LocalDate.of(2023, Month.JANUARY, 1))
        periodeFraJan2023.resultat.finnAnvendtRegelverkBarnepensjon() shouldBe Regelverk.REGELVERK_TOM_DES_2023

        val periodeFraMai2023 = periodiserteResultater.fraDato(LocalDate.of(2023, Month.MAY, 1))
        periodeFraMai2023.resultat.finnAnvendtRegelverkBarnepensjon() shouldBe Regelverk.REGELVERK_TOM_DES_2023

        // Perioder fom 2024 skal returnere BP_REGELVERK_FOM_2024

        val periodeFraJan2024 = periodiserteResultater.fraDato(LocalDate.of(2024, Month.JANUARY, 1))
        periodeFraJan2024.resultat.finnAnvendtRegelverkBarnepensjon() shouldBe Regelverk.REGELVERK_FOM_JAN_2024

        val periodeFraMai2024 = periodiserteResultater.fraDato(LocalDate.of(2024, Month.MAY, 1))
        periodeFraMai2024.resultat.finnAnvendtRegelverkBarnepensjon() shouldBe Regelverk.REGELVERK_FOM_JAN_2024
    }

    private fun <S> List<PeriodisertResultat<S>>.fraDato(dato: LocalDate): PeriodisertResultat<S> = first { it.periode.fraDato == dato }

    private fun opprettBeregningsgrunnlag() =
        PeriodisertBarnepensjonGrunnlag(
            soeskenKull = KonstantGrunnlag(FaktumNode(emptyList(), kilde, "Ingen søsken i kullet")),
            avdoedesTrygdetid =
                KonstantGrunnlag(
                    FaktumNode(
                        listOf(
                            AnvendtTrygdetid(
                                beregningsMetode = BeregningsMetode.NASJONAL,
                                trygdetid = Beregningstall(MAKS_TRYGDETID),
                                ident = AVDOED_FOEDSELSNUMMER.value,
                            ),
                        ),
                        kilde,
                        "trygdetid",
                    ),
                ),
            institusjonsopphold = KonstantGrunnlag(FaktumNode(null, kilde, "Ingen institusjonsopphold")),
            kunEnJuridiskForelder = KonstantGrunnlag(FaktumNode(false, kilde, "Kun en registrert juridisk forelder")),
        )
}
