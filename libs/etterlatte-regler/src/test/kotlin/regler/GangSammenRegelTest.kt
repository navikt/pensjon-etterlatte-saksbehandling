package regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class GangSammenRegelTest {

    private data class TestGrunnlag(override val periode: FaktumNode<RegelPeriode>) : RegelGrunnlag

    private val regel1 = definerKonstant<TestGrunnlag, BigDecimal>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 2",
        regelReferanse = ToDoRegelReferanse(),
        verdi = 2.toBigDecimal()
    )

    private val regel2 = definerKonstant<TestGrunnlag, BigDecimal>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 3",
        regelReferanse = ToDoRegelReferanse(),
        verdi = 3.toBigDecimal()
    )

    private val regel3 = definerKonstant<TestGrunnlag, BigDecimal>(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Tallet 4",
        regelReferanse = ToDoRegelReferanse(),
        verdi = 4.toBigDecimal()
    )

    private val regelSomMultiplisererTreVerdier = RegelMeta(
        gjelderFra = GJELDER_FRA,
        beskrivelse = "Regel som bruker resultatet av tre andre regler og multipliserer disse",
        regelReferanse = ToDoRegelReferanse()
    ) multipliser (regel1 og regel2 og regel3)

    @Test
    fun `skal multiplisere verdiene fra tre regler`() {
        val resultat = regelSomMultiplisererTreVerdier.anvend(GRUNNLAG)

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
        private val SAKSBEHANDLER = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
        private val GRUNNLAG = TestGrunnlag(
            FaktumNode(RegelPeriode(LocalDate.of(2030, 1, 1)), SAKSBEHANDLER, "virkningstidspunkt")
        )
    }
}