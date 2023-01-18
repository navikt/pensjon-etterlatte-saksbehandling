package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.beregning.regler.sats.grunnbeloep
import no.nav.etterlatte.libs.regler.Node
import no.nav.etterlatte.libs.regler.SubsumsjonsNode
import no.nav.etterlatte.libs.regler.Visitor
import java.math.BigDecimal

class FinnAnvendtGrunnbeloepVisitor : Visitor {
    var anvendtGrunnbeloep: Int? = null
    override fun visit(node: Node<*>) {}
    override fun visit(node: SubsumsjonsNode<*>) {
        if (node.regel === grunnbeloep && node.verdi is BigDecimal) {
            anvendtGrunnbeloep = (node.verdi as BigDecimal).toInt()
        }
    }
}

fun SubsumsjonsNode<*>.finnAnvendtGrunnbeloep(): Int? =
    with(FinnAnvendtGrunnbeloepVisitor()) {
        accept(this)
        anvendtGrunnbeloep
    }