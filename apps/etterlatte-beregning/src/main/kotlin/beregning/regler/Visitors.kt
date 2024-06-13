package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.regler.Node
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.SubsumsjonsNode
import no.nav.etterlatte.libs.regler.Visitor

class FinnAnvendtGrunnbeloepVisitor(
    private val grunnbeloepRegel: Regel<*, *>,
) : Visitor {
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

class FinnAnvendtTrygdetidVisitor(
    private val trygdetidRegel: Regel<*, *>,
) : Visitor {
    var anvendtTrygdetid: AnvendtTrygdetid? = null

    override fun visit(node: Node<*>) {}

    override fun visit(node: SubsumsjonsNode<*>) {
        if (node.regel === trygdetidRegel && node.verdi is AnvendtTrygdetid) {
            anvendtTrygdetid = (node.verdi as AnvendtTrygdetid)
        }
    }
}

fun SubsumsjonsNode<*>.finnAnvendtTrygdetid(trygdetidRegel: Regel<*, *>): AnvendtTrygdetid? =
    with(FinnAnvendtTrygdetidVisitor(trygdetidRegel)) {
        accept(this)
        anvendtTrygdetid
    }

class FinnAvdodeForeldre2024Visitor(
    private val avdodeForeldre2024Regel: Regel<*, *>,
) : Visitor {
    var avdodeForeldre: List<String?>? = null

    override fun visit(node: Node<*>) {}

    override fun visit(node: SubsumsjonsNode<*>) {
        if (node.regel === avdodeForeldre2024Regel && node.verdi is List<*>) {
            @Suppress("UNCHECKED_CAST")
            avdodeForeldre = (node.verdi as List<String?>)
        }
    }
}

fun SubsumsjonsNode<*>.finnAvdodeForeldre(avdodeForeldre2024Regel: Regel<*, *>): List<String?>? =
    with(FinnAvdodeForeldre2024Visitor(avdodeForeldre2024Regel)) {
        accept(this)
        avdodeForeldre
    }
