package regler

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.Instant

sealed class Node<T>(
    open val verdi: T,
    val opprettet: Instant = Instant.now()
) {
    abstract fun accept(visitor: Visitor)
}

data class SubsumsjonsNode<T>(
    override val verdi: T,
    val regel: Regel<*, *>,
    val children: List<Node<*>>
) : Node<T>(verdi) {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        if (visitor is RegelVisitor) regel.accept(visitor)
        children.forEach { it.accept(visitor) }
    }
}

data class FaktumNode<T>(
    override val verdi: T,
    val kilde: Grunnlagsopplysning.Kilde,
    val beskrivelse: String
) : Node<T>(verdi) {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}