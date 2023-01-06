package no.nav.etterlatte.libs.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class RegelPeriode(val fraDato: LocalDate, val tilDato: LocalDate? = null) {
    init {
        assert(tilDato == null || tilDato >= fraDato) { "Tildato må være større eller lik fradato" }
    }
}

interface RegelReferanse {
    val id: String
    val beskrivelse: String
}

data class LesInputReferanse(override val id: String = "INPUT", override val beskrivelse: String) :
    RegelReferanse

abstract class Regel<G, S>(
    open val gjelderFra: LocalDate,
    open val beskrivelse: String,
    open val regelReferanse: RegelReferanse
) {
    fun anvend(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<S> =
        if (gjelderFra <= periode.fraDato) {
            anvendRegel(grunnlag, periode)
        } else {
            throw IngenGyldigeReglerForTidspunktException(periode)
        }

    protected abstract fun anvendRegel(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<S>
    abstract fun accept(visitor: RegelVisitor)
}

open class SlaaSammenToRegler<C, D, G, S, A : Regel<G, C>, B : Regel<G, D>>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regel1: A,
    @JsonIgnore
    val regel2: B,
    @JsonIgnore
    val slaasammenFunksjon: (C, D) -> S
) : Regel<G, S>(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse
) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regel1.accept(visitor)
        regel2.accept(visitor)
    }

    override fun anvendRegel(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<S> {
        val verdi1 = regel1.anvend(grunnlag, periode)
        val verdi2 = regel2.anvend(grunnlag, periode)
        return SubsumsjonsNode(
            verdi = slaasammenFunksjon(verdi1.verdi, verdi2.verdi),
            regel = this,
            children = listOf(verdi1, verdi2)
        )
    }
}

open class SlaaSammenTreRegler<D, E, F, G, S, A : Regel<G, D>, B : Regel<G, E>, C : Regel<G, F>>(
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
) : Regel<G, S>(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse
) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regel1.accept(visitor)
        regel2.accept(visitor)
        regel3.accept(visitor)
    }

    override fun anvendRegel(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<S> {
        val verdi1 = regel1.anvend(grunnlag, periode)
        val verdi2 = regel2.anvend(grunnlag, periode)
        val verdi3 = regel3.anvend(grunnlag, periode)
        return SubsumsjonsNode(
            verdi = slaasammenFunksjon(verdi1.verdi, verdi2.verdi, verdi3.verdi),
            regel = this,
            children = listOf(verdi1, verdi2, verdi3)
        )
    }
}

open class VelgNyesteGyldigRegel<G, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regler: List<Regel<G, S>>
) : Regel<G, S>(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse
) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)

        when (visitor) {
            is KanAnvendesPaaPeriode -> {
                regler.nyesteGyldigeRegel(visitor.periode)?.accept(visitor)
            }
            else -> regler.forEach { regel -> regel.accept(visitor) }
        }
    }

    override fun anvendRegel(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<S> {
        val regel = regler.nyesteGyldigeRegel(periode)?.anvend(grunnlag, periode)
            ?: throw IngenGyldigeReglerForTidspunktException(periode)

        return SubsumsjonsNode(
            verdi = regel.verdi,
            regel = this,
            children = listOf(regel)
        )
    }

    private fun <G, S> List<Regel<G, S>>.nyesteGyldigeRegel(periode: RegelPeriode) = this
        .filter { regel -> regel.gjelderFra <= periode.fraDato }
        .maxByOrNull { it.gjelderFra }
}

open class GangSammenRegel<G>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regler: List<Regel<G, BigDecimal>>
) : Regel<G, BigDecimal>(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse
) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regler.forEach { regel -> regel.accept(visitor) }
    }

    override fun anvendRegel(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<BigDecimal> {
        val noder = regler.map { it.anvend(grunnlag, periode) }
        val verdi = noder.map { r -> r.verdi }.reduce { acc, i -> acc * i }

        return SubsumsjonsNode(
            verdi = verdi,
            regel = this,
            children = noder
        )
    }
}

open class FinnFaktumIGrunnlagRegel<G, T, A : FaktumNode<T>, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val finnFaktum: (G) -> A,
    @JsonIgnore
    val finnFelt: (T) -> S
) : Regel<G, S>(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse
) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
    }

    override fun anvendRegel(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<S> {
        val faktum = finnFaktum(grunnlag)
        return SubsumsjonsNode(
            verdi = finnFelt(faktum.verdi),
            regel = this,
            children = listOf(faktum)
        )
    }
}

open class KonstantRegel<G, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    private val verdi: S
) : Regel<G, S>(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse
) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
    }

    override fun anvendRegel(grunnlag: G, periode: RegelPeriode): SubsumsjonsNode<S> = SubsumsjonsNode(
        verdi = verdi,
        regel = this,
        children = listOf()
    )
}

data class IngenGyldigeReglerForTidspunktException(val periode: RegelPeriode) :
    Exception("Ingen gyldige regler er konfigurert for tidsrommet: ${periode.fraDato} - ${periode.tilDato}")