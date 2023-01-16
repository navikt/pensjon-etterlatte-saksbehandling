package no.nav.etterlatte.libs.regler

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TransformasjonRegelTest {

    private object TestGrunnlag

    private val regel1 = definerKonstant<TestGrunnlag, Int>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 5",
        regelReferanse = toDoRegelReferanse,
        verdi = 5
    )

    private val transformasjonsRegel = RegelMeta(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Regel som transformerer en annen regels resultat",
        regelReferanse = toDoRegelReferanse
    ) benytter regel1 med { regel1Verdi ->
        regel1Verdi * 2
    }

    @Test
    fun `Skal transformere resultatet av en annen regel`() {
        val resultat =
            transformasjonsRegel.anvend(TestGrunnlag, RegelPeriode(LocalDate.of(2030, 1, 1)))

        with(resultat) {
            verdi shouldBe 10
            regel.gjelderFra shouldBe transformasjonsRegel.gjelderFra
            regel.beskrivelse shouldBe transformasjonsRegel.beskrivelse
            regel.regelReferanse shouldBe transformasjonsRegel.regelReferanse
            noder.size shouldBe 1
        }
    }

    private companion object {
        private val GJELDER_FRA = LocalDate.of(2030, 1, 1)
    }
}