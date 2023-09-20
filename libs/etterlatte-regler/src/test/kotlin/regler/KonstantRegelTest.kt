package no.nav.etterlatte.libs.regler

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KonstantRegelTest {
    private object TestGrunnlag

    private val regelSomReturnererKonstantVerdi =
        definerKonstant<TestGrunnlag, Int>(
            gjelderFra = GJELDER_FRA,
            beskrivelse = "Tallet 2",
            regelReferanse = regelReferanse,
            verdi = 2,
        )

    @Test
    fun `skal returnere konstant verdi`() {
        val resultat = regelSomReturnererKonstantVerdi.anvend(TestGrunnlag, RegelPeriode(LocalDate.of(2030, 1, 1)))

        with(resultat) {
            verdi shouldBe 2
            regel.gjelderFra shouldBe regelSomReturnererKonstantVerdi.gjelderFra
            regel.beskrivelse shouldBe regelSomReturnererKonstantVerdi.beskrivelse
            regel.regelReferanse shouldBe regelSomReturnererKonstantVerdi.regelReferanse
        }
    }

    private companion object {
        private val GJELDER_FRA = LocalDate.of(2030, 1, 1)
    }
}
