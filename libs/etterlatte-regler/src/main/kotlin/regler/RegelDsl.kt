package regler

import FaktumNode
import FinnFaktumIGrunnlagRegel
import Konstant
import Regel
import RegelReferanse
import SlaaSammenToRegler
import java.math.BigDecimal

data class RegelMeta(
    val versjon: String,
    val beskrivelse: String,
    val regelReferanse: RegelReferanse
)

fun <C : Any, D : Any, G, S, A : Regel<G, C>, B : Regel<G, D>> slaaSammenToRegler(
    versjon: String,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel1: A,
    regel2: B,
    slaaSammenFunksjon: (C, D) -> S
) = SlaaSammenToRegler(
    versjon = versjon,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    venstre = regel1,
    hoeyre = regel2,
    slaasammenFunksjon = slaaSammenFunksjon
)

fun <G, A : Regel<G, BigDecimal>, B : Regel<G, BigDecimal>> gangSammenToRegler(
    versjon: String,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel1: A,
    regel2: B
) = SlaaSammenToRegler(
    versjon = versjon,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    venstre = regel1,
    hoeyre = regel2,
    slaasammenFunksjon = { a: BigDecimal, b: BigDecimal -> a * b }
)

fun <G, T : Any, A : FaktumNode<T>, S> finnFaktumIGrunnlag(
    versjon: String,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    finnFaktum: (G) -> A,
    finnFelt: (T) -> S
): Regel<G, S> = FinnFaktumIGrunnlagRegel(
    versjon,
    beskrivelse,
    regelReferanse,
    finnFaktum,
    finnFelt
)

fun <G, S> definerKonstant(
    versjon: String,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    verdi: S
): Regel<G, S> = Konstant(
    versjon,
    beskrivelse,
    regelReferanse,
    verdi
)

infix fun <G, A> RegelMeta.kombinerer(regel1: Regel<G, A>) = this to regel1
infix fun <G, A> RegelMeta.multipliser(regel1: Regel<G, A>) = this to regel1
infix fun <G, A, B> Pair<RegelMeta, Regel<G, A>>.og(regel2: Regel<G, B>) = this to regel2
infix fun <G, A : Any, B : Any, S> Pair<Pair<RegelMeta, Regel<G, A>>, Regel<G, B>>.med(f: (A, B) -> S) =
    slaaSammenToRegler(
        versjon = first.first.versjon,
        beskrivelse = first.first.beskrivelse,
        regelReferanse = first.first.regelReferanse,
        regel1 = first.second,
        regel2 = second,
        slaaSammenFunksjon = f
    )

infix fun <G> Pair<RegelMeta, Regel<G, BigDecimal>>.med(b: Regel<G, BigDecimal>) =
    gangSammenToRegler(
        versjon = first.versjon,
        beskrivelse = first.beskrivelse,
        regelReferanse = first.regelReferanse,
        regel1 = second,
        regel2 = b
    )