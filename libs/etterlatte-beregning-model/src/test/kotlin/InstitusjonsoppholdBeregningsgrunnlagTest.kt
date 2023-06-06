import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Prosent
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstitusjonsoppholdBeregningsgrunnlagTest {

    @Test
    fun `prosentEtterReduksjon returnerer 100 for ikke redusert`() {
        assertEquals(
            Prosent.hundre,
            InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD).prosentEtterReduksjon()
        )
    }

    @Test
    fun `prosentEtterReduksjon returnerer 10 for vanlig redusert`() {
        assertEquals(Prosent(10), InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG).prosentEtterReduksjon())
    }

    @Test
    fun `prosentEtterReduksjon returnerer hundre minus egendefinert for egendefinert reduksjon`() {
        assertEquals(
            Prosent(60),
            InstitusjonsoppholdBeregningsgrunnlag(
                Reduksjon.JA_EGEN_PROSENT_AV_G,
                egenReduksjon = 40
            ).prosentEtterReduksjon()
        )
    }
}