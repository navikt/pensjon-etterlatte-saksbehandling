import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

interface RegelGrunnlag {
    val virkningstidspunkt: FaktumNode<YearMonth>
}

sealed class Node<T>(
    open val verdi: T,
    val opprettet: Instant = Instant.now()
) {
    abstract fun accept(visitor: Visitor)
}

data class SubsumsjonsNode<T>(
    override val verdi: T,
    val regel: Regel<*, *>,
    val children: List<Node<out Any>>
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

interface RegelReferanse
data class ToDoRegelReferanse(val beskrivelse: String = "ToDo: Legg til referanse") : RegelReferanse

interface Regel<G : RegelGrunnlag, S> {
    val gjelderFra: LocalDate
    val beskrivelse: String
    val regelReferanse: RegelReferanse

    fun anvend(grunnlag: G): SubsumsjonsNode<S>
    fun accept(visitor: RegelVisitor)
}

open class SlaaSammenToRegler<C : Any, D : Any, G : RegelGrunnlag, S, A : Regel<G, C>, B : Regel<G, D>>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val venstre: A,
    @JsonIgnore
    val hoeyre: B,
    @JsonIgnore
    val slaasammenFunksjon: (C, D) -> S
) : Regel<G, S> {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        venstre.accept(visitor)
        hoeyre.accept(visitor)
    }

    override fun anvend(grunnlag: G): SubsumsjonsNode<S> {
        val verdi1 = venstre.anvend(grunnlag)
        val verdi2 = hoeyre.anvend(grunnlag)
        return SubsumsjonsNode(
            verdi = slaasammenFunksjon(verdi1.verdi, verdi2.verdi),
            regel = this,
            children = listOf(verdi1, verdi2)
        )
    }
}

open class VelgNyesteGyldigRegel<G : RegelGrunnlag, S : Any>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regler: List<Regel<G, S>>
) : Regel<G, S> {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regler.forEach { regel -> regel.accept(visitor) }
    }

    override fun anvend(grunnlag: G): SubsumsjonsNode<S> {
        val regel = regler
            .filter { it.gjelderFra <= grunnlag.virkningstidspunkt.verdi.atDay(1) }
            .maxBy { it.gjelderFra }
            .anvend(grunnlag)

        return SubsumsjonsNode(
            verdi = regel.verdi,
            regel = this,
            children = listOf(regel)
        )
    }
}

open class FinnFaktumIGrunnlagRegel<G : RegelGrunnlag, T : Any, A : FaktumNode<T>, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val finnFaktum: (G) -> A,
    @JsonIgnore
    val finnFelt: (T) -> S
) : Regel<G, S> {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
    }

    override fun anvend(grunnlag: G): SubsumsjonsNode<S> {
        val faktum = finnFaktum(grunnlag)
        return SubsumsjonsNode(
            verdi = finnFelt(faktum.verdi),
            regel = this,
            children = listOf(faktum)
        )
    }
}

open class Konstant<G : RegelGrunnlag, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    private val verdi: S
) : Regel<G, S> {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
    }

    override fun anvend(grunnlag: G): SubsumsjonsNode<S> = SubsumsjonsNode(
        verdi = verdi,
        regel = this,
        children = listOf()
    )
}

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