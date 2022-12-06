package regler

import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

interface RegelGrunnlag {
    val virkningstidspunkt: FaktumNode<YearMonth>
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
    val regel1: A,
    @JsonIgnore
    val regel2: B,
    @JsonIgnore
    val slaasammenFunksjon: (C, D) -> S
) : Regel<G, S> {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regel1.accept(visitor)
        regel2.accept(visitor)
    }

    override fun anvend(grunnlag: G): SubsumsjonsNode<S> {
        val verdi1 = regel1.anvend(grunnlag)
        val verdi2 = regel2.anvend(grunnlag)
        return SubsumsjonsNode(
            verdi = slaasammenFunksjon(verdi1.verdi, verdi2.verdi),
            regel = this,
            children = listOf(verdi1, verdi2)
        )
    }
}

open class SlaaSammenTreRegler<D : Any, E : Any, F : Any, G : RegelGrunnlag, S, A : Regel<G, D>, B : Regel<G, E>, C :
        Regel<G, F>>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regel1: A,
    @JsonIgnore
    val regel2: B,
    @JsonIgnore
    val regel3: C,
    @JsonIgnore
    val slaasammenFunksjon: (D, E, F) -> S
) : Regel<G, S> {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regel1.accept(visitor)
        regel2.accept(visitor)
        regel3.accept(visitor)
    }

    override fun anvend(grunnlag: G): SubsumsjonsNode<S> {
        val verdi1 = regel1.anvend(grunnlag)
        val verdi2 = regel2.anvend(grunnlag)
        val verdi3 = regel3.anvend(grunnlag)
        return SubsumsjonsNode(
            verdi = slaasammenFunksjon(verdi1.verdi, verdi2.verdi, verdi3.verdi),
            regel = this,
            children = listOf(verdi1, verdi2, verdi3)
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

open class GangSammenRegel<G : RegelGrunnlag>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regler: List<Regel<G, BigDecimal>>
) : Regel<G, BigDecimal> {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regler.forEach { regel -> regel.accept(visitor) }
    }

    override fun anvend(grunnlag: G): SubsumsjonsNode<BigDecimal> {
        val noder = regler.map { it.anvend(grunnlag) }
        val verdi = noder.map { r -> r.verdi }.reduce { acc, i -> acc * i }

        return SubsumsjonsNode(
            verdi = verdi,
            regel = this,
            children = noder
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