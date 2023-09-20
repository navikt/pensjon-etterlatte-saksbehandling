package no.nav.etterlatte.libs.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

data class RegelPeriode(val fraDato: LocalDate, val tilDato: LocalDate? = null) {
    init {
        assert(tilDato == null || tilDato >= fraDato) { "Tildato må være større eller lik fradato" }
    }
}

data class RegelReferanse(val id: String, val versjon: String = "1")

abstract class Regel<G, S>(
    open val gjelderFra: LocalDate,
    open val beskrivelse: String,
    open val regelReferanse: RegelReferanse,
) {
    fun anvend(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S> =
        if (gjelderFra <= periode.fraDato) {
            anvendRegel(grunnlag, periode)
        } else {
            throw IngenGyldigeReglerForTidspunktException(periode)
        }

    protected abstract fun anvendRegel(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S>

    abstract fun accept(visitor: RegelVisitor)
}

open class TransformerEnRegel<G, S, S1, R : Regel<G, S1>>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regel: R,
    @JsonIgnore
    val transformer: (S1) -> S,
) : Regel<G, S>(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = regelReferanse,
    ) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regel.accept(visitor)
    }

    override fun anvendRegel(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S> {
        val verdi = regel.anvend(grunnlag, periode)
        return SubsumsjonsNode(
            verdi = transformer(verdi.verdi),
            regel = this,
            noder = listOf(verdi),
        )
    }
}

open class TransformerToRegler<G, S, S1, S2, R1 : Regel<G, S1>, R2 : Regel<G, S2>>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regel1: R1,
    @JsonIgnore
    val regel2: R2,
    @JsonIgnore
    val transformer: (S1, S2) -> S,
) : Regel<G, S>(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = regelReferanse,
    ) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regel1.accept(visitor)
        regel2.accept(visitor)
    }

    override fun anvendRegel(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S> {
        val verdi1 = regel1.anvend(grunnlag, periode)
        val verdi2 = regel2.anvend(grunnlag, periode)
        return SubsumsjonsNode(
            verdi = transformer(verdi1.verdi, verdi2.verdi),
            regel = this,
            noder = listOf(verdi1, verdi2),
        )
    }
}

open class TransformerTreRegler<S1, S2, S3, G, S, R1 : Regel<G, S1>, R2 : Regel<G, S2>, R3 : Regel<G, S3>>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regel1: R1,
    @JsonIgnore
    val regel2: R2,
    @JsonIgnore
    val regel3: R3,
    @JsonIgnore
    val transformer: (S1, S2, S3) -> S,
) : Regel<G, S>(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = regelReferanse,
    ) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
        regel1.accept(visitor)
        regel2.accept(visitor)
        regel3.accept(visitor)
    }

    override fun anvendRegel(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S> {
        val verdi1 = regel1.anvend(grunnlag, periode)
        val verdi2 = regel2.anvend(grunnlag, periode)
        val verdi3 = regel3.anvend(grunnlag, periode)
        return SubsumsjonsNode(
            verdi = transformer(verdi1.verdi, verdi2.verdi, verdi3.verdi),
            regel = this,
            noder = listOf(verdi1, verdi2, verdi3),
        )
    }
}

open class VelgNyesteGyldigRegel<G, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    @JsonIgnore
    val regler: List<Regel<G, S>>,
) : Regel<G, S>(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = regelReferanse,
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

    override fun anvendRegel(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S> {
        val regel =
            regler.nyesteGyldigeRegel(periode)?.anvend(grunnlag, periode)
                ?: throw IngenGyldigeReglerForTidspunktException(periode)

        return SubsumsjonsNode(
            verdi = regel.verdi,
            regel = this,
            noder = listOf(regel),
        )
    }

    private fun <G, S> List<Regel<G, S>>.nyesteGyldigeRegel(periode: RegelPeriode) =
        this
            .filter { regel -> regel.gjelderFra <= periode.fraDato }
            .maxByOrNull { it.gjelderFra }
}

open class FinnFaktumIGrunnlagRegel<G, T, F : FaktumNode<T>, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    @JsonIgnore
    val finnFaktum: (G) -> F,
    @JsonIgnore
    val finnFelt: (T) -> S,
) : Regel<G, S>(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = RegelReferanse(id = "INPUT"),
    ) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
    }

    override fun anvendRegel(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S> {
        val faktum = finnFaktum(grunnlag)
        return SubsumsjonsNode(
            verdi = finnFelt(faktum.verdi),
            regel = this,
            noder = listOf(faktum),
        )
    }
}

open class KonstantRegel<G, S>(
    override val gjelderFra: LocalDate,
    override val beskrivelse: String,
    override val regelReferanse: RegelReferanse,
    private val verdi: S,
) : Regel<G, S>(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = regelReferanse,
    ) {
    override fun accept(visitor: RegelVisitor) {
        visitor.visit(this)
    }

    override fun anvendRegel(
        grunnlag: G,
        periode: RegelPeriode,
    ): SubsumsjonsNode<S> =
        SubsumsjonsNode(
            verdi = verdi,
            regel = this,
            noder = listOf(),
        )
}

data class IngenGyldigeReglerForTidspunktException(val periode: RegelPeriode) :
    Exception("Ingen gyldige regler er konfigurert for tidsrommet: ${periode.fraDato} - ${periode.tilDato}")
