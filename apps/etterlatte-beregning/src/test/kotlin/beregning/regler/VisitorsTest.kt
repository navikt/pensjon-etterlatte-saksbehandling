package no.nav.etterlatte.beregning.regler.beregning.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.barnepensjon.beregnBarnepensjon
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.finnAnvendtRegelverkBarnepensjon
import no.nav.etterlatte.libs.common.beregning.Regelverk
import no.nav.etterlatte.libs.regler.RegelPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class VisitorsTest {
    @Test
    fun `finnAnvendtRegelverkBarnepensjon skal returnere BP_REGELVERK_TOM_2023`() {
        val resultat =
            beregnBarnepensjon.anvend(
                grunnlag = barnepensjonGrunnlag(),
                periode = RegelPeriode(fraDato = LocalDate.of(2023, Month.JANUARY, 1)),
            )

        resultat.finnAnvendtRegelverkBarnepensjon() shouldBe Regelverk.BP_REGELVERK_TOM_2023
    }

    @Test
    fun `finnAnvendtRegelverkBarnepensjon skal returnere BP_REGELVERK_FOM_2024`() {
        val resultat =
            beregnBarnepensjon.anvend(
                grunnlag = barnepensjonGrunnlag(),
                periode = RegelPeriode(fraDato = LocalDate.of(2024, Month.JANUARY, 1)),
            )

        resultat.finnAnvendtRegelverkBarnepensjon() shouldBe Regelverk.BP_REGELVERK_FOM_2024
    }
}
