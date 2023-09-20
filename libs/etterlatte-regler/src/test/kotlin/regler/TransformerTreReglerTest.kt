package no.nav.etterlatte.libs.regler

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TransformerTreReglerTest {
    private object TestGrunnlag

    private val regel1 =
        definerKonstant<TestGrunnlag, Int>(
            gjelderFra = GJELDER_FRA,
            beskrivelse = "Tallet 1",
            regelReferanse = regelReferanse,
            verdi = 1,
        )

    private val regel2 =
        definerKonstant<TestGrunnlag, String>(
            gjelderFra = GJELDER_FRA,
            beskrivelse = "Tallet 2 som string",
            regelReferanse = regelReferanse,
            verdi = "2",
        )

    private val regel3 =
        definerKonstant<TestGrunnlag, Long>(
            gjelderFra = GJELDER_FRA,
            beskrivelse = "Tallet 3",
            regelReferanse = regelReferanse,
            verdi = 3,
        )

    private val regelSomBrukerVerdienFraTreAndreRegler =
        RegelMeta(
            gjelderFra = GJELDER_FRA,
            beskrivelse = "Regel som bruker resultatet av tre andre regler",
            regelReferanse = regelReferanse,
        ) benytter regel1 og regel2 og regel3 med { verdi1, verdi2, verdi3 ->
            verdi1 + verdi2.toInt() + verdi3
        }

    @Test
    fun `skal bruke resultatet av tre regler som grunnlag til en ny regel`() {
        val resultat =
            regelSomBrukerVerdienFraTreAndreRegler.anvend(TestGrunnlag, RegelPeriode(LocalDate.of(2030, 1, 1)))

        with(resultat) {
            verdi shouldBe 6
            regel.gjelderFra shouldBe regelSomBrukerVerdienFraTreAndreRegler.gjelderFra
            regel.beskrivelse shouldBe regelSomBrukerVerdienFraTreAndreRegler.beskrivelse
            regel.regelReferanse shouldBe regelSomBrukerVerdienFraTreAndreRegler.regelReferanse
            noder.size shouldBe 3
        }
    }

    private companion object {
        private val GJELDER_FRA = LocalDate.of(2030, 1, 1)
    }
}
