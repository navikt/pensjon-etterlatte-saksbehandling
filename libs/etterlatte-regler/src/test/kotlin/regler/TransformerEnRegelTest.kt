package no.nav.etterlatte.libs.regler

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TransformerEnRegelTest {
    private object TestGrunnlag

    private val regel1 =
        definerKonstant<TestGrunnlag, Int>(
            gjelderFra = GJELDER_FRA,
            beskrivelse = "Tallet 5",
            regelReferanse = regelReferanse,
            verdi = 5,
        )

    private val regelSomBrukerVerdienFraEnAnnenRegel =
        RegelMeta(
            gjelderFra = GJELDER_FRA,
            beskrivelse = "Regel som transformerer en annen regels resultat",
            regelReferanse = regelReferanse,
        ) benytter regel1 med { regel1Verdi ->
            regel1Verdi * 2
        }

    @Test
    fun `Skal transformere resultatet av en annen regel`() {
        val resultat =
            regelSomBrukerVerdienFraEnAnnenRegel.anvend(TestGrunnlag, RegelPeriode(LocalDate.of(2030, 1, 1)))

        with(resultat) {
            verdi shouldBe 10
            regel.gjelderFra shouldBe regelSomBrukerVerdienFraEnAnnenRegel.gjelderFra
            regel.beskrivelse shouldBe regelSomBrukerVerdienFraEnAnnenRegel.beskrivelse
            regel.regelReferanse shouldBe regelSomBrukerVerdienFraEnAnnenRegel.regelReferanse
            noder.size shouldBe 1
        }
    }

    private companion object {
        private val GJELDER_FRA = LocalDate.of(2030, 1, 1)
    }
}
