package no.nav.etterlatte.libs.regler

import java.time.LocalDate

interface Visitor {
    fun visit(node: Node<*>)

    fun visit(node: SubsumsjonsNode<*>)
}

interface RegelVisitor {
    fun visit(regel: Regel<*, *>)
}

class FinnAlleReglerVisitor : Visitor {
    val regler = mutableListOf<Regel<*, *>>()

    override fun visit(node: Node<*>) {}

    override fun visit(node: SubsumsjonsNode<*>) {
        regler += node.regel
    }
}

fun Node<*>.finnAnvendteRegler(): List<Regel<*, *>> {
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

class KanAnvendesPaaPeriode(val periode: RegelPeriode) : RegelVisitor {
    val ugyldigeReglerForPeriode = mutableListOf<Regel<*, *>>()

    override fun visit(regel: Regel<*, *>) {
        when (regel) {
            is VelgNyesteGyldigRegel<*, *> -> {
                if (regel.regler.none { periode.fraDato >= it.gjelderFra }) {
                    ugyldigeReglerForPeriode.add(regel)
                }
            }
            else -> {
                if (periode.fraDato < regel.gjelderFra) {
                    ugyldigeReglerForPeriode.add(regel)
                }
            }
        }
    }
}

fun Regel<*, *>.finnUgyldigePerioder(periode: RegelPeriode): List<Regel<*, *>> {
    val resultat = KanAnvendesPaaPeriode(periode)

    accept(resultat)

    return resultat.ugyldigeReglerForPeriode
}
