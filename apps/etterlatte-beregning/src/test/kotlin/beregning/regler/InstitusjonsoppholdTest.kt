package no.nav.etterlatte.beregning.regler.beregning.regler

import beregning.regler.barnepensjon.institusjonsoppholdRegel
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.REGEL_PERIODE
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import org.junit.jupiter.api.Test

class InstitusjonsoppholdTest {

    @Test
    fun `institusjonsopphold med vanlig reduksjon skal gi 10 prosent`() {
        val resultat = institusjonsoppholdRegel.anvend(
            barnepensjonGrunnlag(institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG)),
            REGEL_PERIODE
        )

        resultat.verdi shouldBe Prosent(10)
    }

    @Test
    fun `institusjonsopphold med egendefinert reduksjon skal gi egendefinert prosent`() {
        val resultat = institusjonsoppholdRegel.anvend(
            barnepensjonGrunnlag(
                institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(
                    Reduksjon.JA_EGEN_PROSENT_AV_G,
                    egenReduksjon = 35
                )
            ),
            REGEL_PERIODE
        )

        resultat.verdi shouldBe Prosent(65)
    }

    @Test
    fun `institusjonsopphold uten reduksjon skal gi 100 prosent`() {
        val resultat = institusjonsoppholdRegel.anvend(
            barnepensjonGrunnlag(institusjonsopphold = null),
            REGEL_PERIODE
        )

        resultat.verdi shouldBe Prosent.hundre
    }
}