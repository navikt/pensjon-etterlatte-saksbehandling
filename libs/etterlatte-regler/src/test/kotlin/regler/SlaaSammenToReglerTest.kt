package no.nav.etterlatte.libs.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class SlaaSammenToReglerTest {

    private object TestGrunnlag

    private val regel1 = definerKonstant<TestGrunnlag, Int>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 1",
        regelReferanse = ToDoRegelReferanse(),
        verdi = 1
    )

    private val regel2 = definerKonstant<TestGrunnlag, String>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 2 som string",
        regelReferanse = ToDoRegelReferanse(),
        verdi = "2"
    )

    private val regelSomBrukerVerdienFraToAndreRegler = RegelMeta(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Regel som bruker resultatet av to andre regler",
        regelReferanse = ToDoRegelReferanse()
    ) kombinerer regel1 og regel2 med { verdi1, verdi2 ->
        verdi1 + verdi2.toInt()
    }

    @Test
    fun `skal bruke resultatet av to regler som grunnlag til en ny regel`() {
        val resultat =
            regelSomBrukerVerdienFraToAndreRegler.anvend(TestGrunnlag, RegelPeriode(LocalDate.of(2030, 1, 1)))

        with(resultat) {
            verdi shouldBe 3
            regel.gjelderFra shouldBe regelSomBrukerVerdienFraToAndreRegler.gjelderFra
            regel.beskrivelse shouldBe regelSomBrukerVerdienFraToAndreRegler.beskrivelse
            regel.regelReferanse shouldBe regelSomBrukerVerdienFraToAndreRegler.regelReferanse
            children.size shouldBe 2
        }
    }

    private companion object {
        private val GJELDER_FRA = LocalDate.of(2030, 1, 1)
        private val SAKSBEHANDLER = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
    }
}