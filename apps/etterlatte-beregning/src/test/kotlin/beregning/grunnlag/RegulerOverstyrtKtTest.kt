package beregning.grunnlag

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.behandling.randomSakId
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
                datoFOM = LocalDate.of(2022, 5, 1),
                datoTOM = null,
                utbetaltBeloep = 5679,
                trygdetid = 40,
                trygdetidForIdent = "",
                prorataBroekTeller = null,
                prorataBroekNevner = null,
                sakId = randomSakId(),
                beskrivelse = "",
                aarsak = "ANNET",
                kilde = Grunnlagsopplysning.Saksbehandler.create(""),
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
