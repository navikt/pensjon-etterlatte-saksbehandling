package regler

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class FinnFaktumIGrunnlagRegelTest {
    data class Grunnlag(val testVerdi2021: FaktumNode<Int>)

    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())

    private val grunnlag = Grunnlag(
        testVerdi2021 = FaktumNode(100_000, saksbehandler, "Verdi for test")
    )

    private val finnFaktumIGrunnlagRegel: Regel<Grunnlag, Int> = finnFaktumIGrunnlag(
        gjelderFra = LocalDate.of(1900, 1, 1),
        beskrivelse = "Finner testverdi for 2021",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Grunnlag::testVerdi2021,
        finnFelt = { it }
    )

    @Test
    fun `Skal hente ut faktum fra grunnlag`() {
        val resultat = finnFaktumIGrunnlagRegel.anvend(grunnlag, RegelPeriode(LocalDate.of(2021, 1, 1)))

        resultat.regel shouldBe finnFaktumIGrunnlagRegel
        resultat.verdi shouldBe 100_000
        resultat.children shouldHaveSize 1
    }
}