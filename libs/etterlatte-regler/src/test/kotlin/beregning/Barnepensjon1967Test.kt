package beregning.barnepensjon1967

import FaktumNode
import Node
import Regel
import RegelVisitor
import SubsumsjonsNode
import Visitor
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import java.time.Instant

class Barnepensjon1967Test {
    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
    private val grunnlag = Barnepensjon1967Grunnlag(
        grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = 100_550L),
        antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
        avdoedForelder = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Avdød forelders trygdetid",
            verdi = AvdoedForelder(trygdetid = 30)
        )
    )

    @Test
    fun `Regler skal representeres som et tre`() {
        reduksjonMotFolketrygdRegel.accept(object : RegelVisitor {
            override fun visit(node: Regel<*, *>) {
                println(node.beskrivelse)
            }
        })
        println("---------------")
        reduksjonMotFolketrygdRegel.anvend(grunnlag).accept(object : Visitor {

            override fun visit(node: Node<*>) {
            }

            override fun visit(node: SubsumsjonsNode<*>) {
                println(node.regel.beskrivelse)
            }
        })

        println(reduksjonMotFolketrygdRegel.anvend(grunnlag).toJson())
    }
}