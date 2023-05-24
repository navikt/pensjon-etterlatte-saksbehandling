package beregning.regler

import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.regler.Node
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.SubsumsjonsNode
import no.nav.etterlatte.libs.regler.Visitor

class FinnAnvendtGrunnbeloepVisitor(val grunnbeloepRegel: Regel<*, *>) : Visitor {
    var anvendtGrunnbeloep: Grunnbeloep? = null
    override fun visit(node: Node<*>) {}
    override fun visit(node: SubsumsjonsNode<*>) {
        if (node.regel === grunnbeloepRegel && node.verdi is Grunnbeloep) {
            anvendtGrunnbeloep = (node.verdi as Grunnbeloep)
        }
    }
}

fun SubsumsjonsNode<*>.finnAnvendtGrunnbeloep(grunnbeloepRegel: Regel<*, *>): Grunnbeloep? =
    with(FinnAnvendtGrunnbeloepVisitor(grunnbeloepRegel)) {
        accept(this)
        anvendtGrunnbeloep
    }