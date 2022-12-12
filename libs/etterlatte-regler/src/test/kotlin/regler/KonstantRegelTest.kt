package regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class KonstantRegelTest {

    private data class TestGrunnlag(override val periode: FaktumNode<RegelPeriode>) : RegelGrunnlag

    private val regelSomReturnererKonstantVerdi = definerKonstant<TestGrunnlag, Int>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 2",
        regelReferanse = ToDoRegelReferanse(),
        verdi = 2
    )

    @Test
    fun `skal returnere konstant verdi`() {
        val resultat = regelSomReturnererKonstantVerdi.anvend(GRUNNLAG)

        with(resultat) {
            verdi shouldBe 2
            regel.gjelderFra shouldBe regelSomReturnererKonstantVerdi.gjelderFra
            regel.beskrivelse shouldBe regelSomReturnererKonstantVerdi.beskrivelse
            regel.regelReferanse shouldBe regelSomReturnererKonstantVerdi.regelReferanse
        }
    }

    private companion object {
        private val GJELDER_FRA = LocalDate.of(2030, 1, 1)
        private val SAKSBEHANDLER = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
        private val GRUNNLAG = TestGrunnlag(
            FaktumNode(RegelPeriode(LocalDate.of(2030, 1, 1)), SAKSBEHANDLER, "virkningstidspunkt")
        )
    }
}