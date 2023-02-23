package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.beregning.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.beregning.regler.barnepensjon.sats.grunnbeloep
import no.nav.etterlatte.libs.regler.Node
import no.nav.etterlatte.libs.regler.SubsumsjonsNode
import no.nav.etterlatte.libs.regler.Visitor

class FinnAnvendtGrunnbeloepVisitor : Visitor {
    var anvendtGrunnbeloep: Grunnbeloep? = null
    override fun visit(node: Node<*>) {}
    override fun visit(node: SubsumsjonsNode<*>) {
        if (node.regel === grunnbeloep && node.verdi is Grunnbeloep) {
            anvendtGrunnbeloep = (node.verdi as Grunnbeloep)
        }
    }
}

fun SubsumsjonsNode<*>.finnAnvendtGrunnbeloep(): Grunnbeloep? =
    with(FinnAnvendtGrunnbeloepVisitor()) {
        accept(this)
        anvendtGrunnbeloep
    }