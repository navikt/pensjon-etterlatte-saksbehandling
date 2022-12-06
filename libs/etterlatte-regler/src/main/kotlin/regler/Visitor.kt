package regler

import java.time.LocalDate

interface Visitor {
    fun visit(node: Node<*>)
    fun visit(node: SubsumsjonsNode<*>)
}

interface RegelVisitor {
    fun visit(regel: Regel<*, *>)
}

class FinnAlleReglerVisitor : Visitor {
    val regler = mutableListOf<String>()
    override fun visit(node: Node<*>) {}
    override fun visit(node: SubsumsjonsNode<*>) {
        regler += node.regel.beskrivelse
    }
}

fun Node<*>.finnAlleRegler(): List<String> {
    val finnAlleReglerVisitor = FinnAlleReglerVisitor()

    accept(finnAlleReglerVisitor)

    return finnAlleReglerVisitor.regler
}

class FinnRegelverkKnekkpunkter : RegelVisitor {
    val knekkpunkter = mutableSetOf<LocalDate>()
    override fun visit(regel: Regel<*, *>) {
        knekkpunkter.add(regel.gjelderFra)
    }
}

fun Regel<*, *>.finnAlleKnekkpunkter(): Set<LocalDate> {
    val finnKnekkpunkterVisitor = FinnRegelverkKnekkpunkter()

    accept(finnKnekkpunkterVisitor)

    return finnKnekkpunkterVisitor.knekkpunkter
}