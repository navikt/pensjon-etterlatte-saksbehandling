package beregning.grunnlag

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.grunnlag.regulerOverstyrtBeregningsgrunnlag
import org.junit.jupiter.api.Test
import java.time.YearMonth

class RegulerOverstyrtKtTest {
    @Test
    fun `beregner regulert overstyrt beregningsgrunnlag`() {
        val resulat = regulerOverstyrtBeregningsgrunnlag(YearMonth.of(2024, 5), 5679)
        resulat shouldBe 6254
    }
}
