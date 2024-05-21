package beregning.grunnlag

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.beregning.grunnlag.OverstyrBeregningGrunnlagDao
import no.nav.etterlatte.beregning.grunnlag.tilpassOverstyrtBeregningsgrunnlagForRegulering
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class RegulerOverstyrtKtTest {
    @Test
    fun `beregner regulert overstyrt beregningsgrunnlag`() {
        val overstyrtBeregningsgrunnlag =
            OverstyrBeregningGrunnlagDao(
                id = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                sakId = 123L,
                datoFOM = LocalDate.of(2022, 5, 1),
                datoTOM = null,
                trygdetid = 40,
                trygdetidForIdent = "",
                prorataBroekNevner = null,
                prorataBroekTeller = null,
                beskrivelse = "",
                kilde = Grunnlagsopplysning.Saksbehandler.create(""),
                utbetaltBeloep = 5679,
            )

        val reguleringsbehandlingId = UUID.randomUUID()
        val resultat =
            tilpassOverstyrtBeregningsgrunnlagForRegulering(
                YearMonth.of(2023, 5),
                overstyrtBeregningsgrunnlag.datoFOM,
                overstyrtBeregningsgrunnlag,
                reguleringsbehandlingId,
            )
        with(resultat) {
            utbetaltBeloep shouldBe 6043
            id shouldNotBe overstyrtBeregningsgrunnlag.id
            behandlingId shouldBe reguleringsbehandlingId
            datoFOM shouldBe overstyrtBeregningsgrunnlag.datoFOM
            datoTOM shouldBe overstyrtBeregningsgrunnlag.datoTOM
            reguleringRegelresultat shouldNotBe null
        }
    }
}
