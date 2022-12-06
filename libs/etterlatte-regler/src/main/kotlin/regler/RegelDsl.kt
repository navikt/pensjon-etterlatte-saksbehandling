package regler

import FaktumNode
import FinnFaktumIGrunnlagRegel
import Konstant
import Regel
import RegelGrunnlag
import RegelReferanse
import SlaaSammenToRegler
import VelgNyesteGyldigRegel
import java.math.BigDecimal
import java.time.LocalDate

data class RegelMeta(
    val gjelderFra: LocalDate,
    val beskrivelse: String,
    val regelReferanse: RegelReferanse
)

fun <C : Any, D : Any, G : RegelGrunnlag, S, A : Regel<G, C>, B : Regel<G, D>> slaaSammenToRegler(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel1: A,
    regel2: B,
    slaaSammenFunksjon: (C, D) -> S
) = SlaaSammenToRegler(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    venstre = regel1,
    hoeyre = regel2,
    slaasammenFunksjon = slaaSammenFunksjon
)

fun <G : RegelGrunnlag, S : Any> velgNyesteRegel(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regler: List<Regel<G, S>>
): Regel<G, S> = VelgNyesteGyldigRegel(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regler = regler
)

fun <G : RegelGrunnlag, A : Regel<G, BigDecimal>, B : Regel<G, BigDecimal>> gangSammenToRegler(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel1: A,
    regel2: B
) = SlaaSammenToRegler(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    venstre = regel1,
    hoeyre = regel2,
    slaasammenFunksjon = { a: BigDecimal, b: BigDecimal -> a * b }
)

fun <G : RegelGrunnlag, T : Any, A : FaktumNode<T>, S> finnFaktumIGrunnlag(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    finnFaktum: (G) -> A,
    finnFelt: (T) -> S
): Regel<G, S> = FinnFaktumIGrunnlagRegel(
    gjelderFra,
    beskrivelse,
    regelReferanse,
    finnFaktum,
    finnFelt
)

fun <G : RegelGrunnlag, S> definerKonstant(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    verdi: S
): Regel<G, S> = Konstant(
    gjelderFra,
    beskrivelse,
    regelReferanse,
    verdi
)

infix fun <G : RegelGrunnlag, A> RegelMeta.kombinerer(regel1: Regel<G, A>) = this to regel1
infix fun <G : RegelGrunnlag, A> RegelMeta.multipliser(regel1: Regel<G, A>) = this to regel1
infix fun <G : RegelGrunnlag, A : Any> RegelMeta.velgNyesteGyldige(regler: List<Regel<G, A>>) = velgNyesteRegel(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regler = regler
)

infix fun <G : RegelGrunnlag, A, B> Pair<RegelMeta, Regel<G, A>>.og(regel2: Regel<G, B>) = this to regel2
infix fun <G : RegelGrunnlag, A : Any> Regel<G, A>.og(that: Regel<G, A>) = listOf(this, that)
infix fun <G : RegelGrunnlag, A : Any> List<Regel<G, A>>.og(that: Regel<G, A>) = this.plus(that)
infix fun <G : RegelGrunnlag, A : Any, B : Any, S> Pair<Pair<RegelMeta, Regel<G, A>>, Regel<G, B>>.med(
    f: (A, B) -> S
) = slaaSammenToRegler(
    gjelderFra = first.first.gjelderFra,
    beskrivelse = first.first.beskrivelse,
    regelReferanse = first.first.regelReferanse,
    regel1 = first.second,
    regel2 = second,
    slaaSammenFunksjon = f
)

infix fun <G : RegelGrunnlag> Pair<RegelMeta, Regel<G, BigDecimal>>.med(b: Regel<G, BigDecimal>) =
    gangSammenToRegler(
        gjelderFra = first.gjelderFra,
        beskrivelse = first.beskrivelse,
        regelReferanse = first.regelReferanse,
        regel1 = second,
        regel2 = b
    )