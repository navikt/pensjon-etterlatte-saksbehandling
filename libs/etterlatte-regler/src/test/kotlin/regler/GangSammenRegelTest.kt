package no.nav.etterlatte.libs.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.regler.beregning.toDoRegelReferanse
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class GangSammenRegelTest {

    private object TestGrunnlag

    private val regel1 = definerKonstant<TestGrunnlag, BigDecimal>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 2",
        regelReferanse = toDoRegelReferanse,
        verdi = 2.toBigDecimal()
    )

    private val regel2 = definerKonstant<TestGrunnlag, BigDecimal>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 3",
        regelReferanse = toDoRegelReferanse,
        verdi = 3.toBigDecimal()
    )

    private val regel3 = definerKonstant<TestGrunnlag, BigDecimal>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 4",
        regelReferanse = toDoRegelReferanse,
        verdi = 4.toBigDecimal()
    )

    private val regelSomMultiplisererTreVerdier = RegelMeta(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Regel som bruker resultatet av tre andre regler og multipliserer disse",
        regelReferanse = toDoRegelReferanse
    ) multipliser (regel1 og regel2 og regel3)

    @Test
    fun `skal multiplisere verdiene fra tre regler`() {
        val resultat = regelSomMultiplisererTreVerdier.anvend(TestGrunnlag, RegelPeriode(LocalDate.of(2030, 1, 1)))

        with(resultat) {
            verdi shouldBe 24.toBigDecimal()
            regel.gjelderFra shouldBe regelSomMultiplisererTreVerdier.gjelderFra
            regel.beskrivelse shouldBe regelSomMultiplisererTreVerdier.beskrivelse
            regel.regelReferanse shouldBe regelSomMultiplisererTreVerdier.regelReferanse
            children.size shouldBe 3
        }
    }

    private companion object {
        private val GJELDER_FRA = LocalDate.of(2030, 1, 1)
    }
}