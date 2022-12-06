package beregning.barnepensjon1967

import FaktumNode
import Node
import Regel
import RegelVisitor
import SubsumsjonsNode
import Visitor
import beregning.AvdoedForelder
import beregning.Barnepensjon1967Grunnlag
import beregning.beregnBarnepensjonRegel
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

class Barnepensjon1967Test {
    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
    private val grunnlag = Barnepensjon1967Grunnlag(
        grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = BigDecimal(100_550)),
        antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
        avdoedForelder = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Avdød forelders trygdetid",
            verdi = AvdoedForelder(trygdetid = BigDecimal(30))
        ),
        virkningstidspunkt = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Virkningstidspunkt",
            verdi = YearMonth.of(2024, 1)
        )
    )

    @Test
    fun `Regler skal representeres som et tre`() {
        beregnBarnepensjonRegel.accept(object : RegelVisitor {
            override fun visit(node: Regel<*, *>) {
                println(node.beskrivelse)
            }
        })
        println("---------------")
        beregnBarnepensjonRegel.anvend(grunnlag).accept(object : Visitor {

            override fun visit(node: Node<*>) {
            }

            override fun visit(node: SubsumsjonsNode<*>) {
                println(node.regel.beskrivelse)
            }
        })

        println(beregnBarnepensjonRegel.anvend(grunnlag).toJson())
    }
}