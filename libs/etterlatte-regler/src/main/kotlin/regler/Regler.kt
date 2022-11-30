import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.Instant

sealed class Node<T>(
    open val verdi: T,
    val opprettet: Instant = Instant.now()
) {
    abstract fun visited(visitor: Visitor)
}

data class RegelNode<T>(
    override val verdi: T,
    val regel: Regel<*, *>,
    val children: List<Node<out Any>>
) : Node<T>(verdi) {
    override fun visited(visitor: Visitor) {
        visitor.visit(this)
        if (visitor is RegelVisitor) regel.visited(visitor)
        children.forEach { it.visited(visitor) }
    }
}

data class FaktumNode<T>(
    override val verdi: T,
    val kilde: Grunnlagsopplysning.Kilde,
    val beskrivelse: String
) : Node<T>(verdi) {
    override fun visited(visitor: Visitor) {
        visitor.visit(this)
    }
}

interface RegelReferanse
data class ToDoRegelReferanse(val beskrivelse: String = "ToDo: Legg til referanse") : RegelReferanse

interface Regel<G, S> {
    val versjon: String
    val beskrivelse: String
    val regelReferanse: RegelReferanse

    fun anvend(grunnlag: G): RegelNode<S>
    fun visited(visitor: RegelVisitor)
}

open class SlaaSammenToRegler<C : Any, D : Any, G, S, A : Regel<G, C>, B : Regel<G, D>>(
    override val versjon: String,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    val regel1: A,
    val regel2: B,
    val slaasammenFunksjon: (C, D) -> S
) : Regel<G, S> {
    override fun visited(visitor: RegelVisitor) {
        visitor.visit(this)
        regel1.visited(visitor)
        regel2.visited(visitor)
    }

    override fun anvend(grunnlag: G): RegelNode<S> {
        val verdi1 = regel1.anvend(grunnlag)
        val verdi2 = regel2.anvend(grunnlag)
        return RegelNode<S>(
            verdi = slaasammenFunksjon(verdi1.verdi, verdi2.verdi),
            regel = this,
            children = listOf(verdi1, verdi2)
        )
    }
}

interface Visitor {
    fun visit(node: Node<*>)
    fun visit(node: RegelNode<*>)
}

interface RegelVisitor {
    fun visit(node: Regel<*, *>)
}

class FinnAlleReglerVisitor : Visitor {
    val regler = mutableListOf<String>()
    override fun visit(node: Node<*>) {}
    override fun visit(node: RegelNode<*>) {
        regler += node.regel.beskrivelse
    }
}

fun Node<*>.finnAlleRegler(): List<String> {
    val finnAlleReglerVisitor = FinnAlleReglerVisitor()

    visited(finnAlleReglerVisitor)

    return finnAlleReglerVisitor.regler
}