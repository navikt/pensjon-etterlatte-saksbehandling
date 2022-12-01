package regler

import FaktumNode
import FinnFaktumIGrunnlagRegel
import Konstant
import Regel
import RegelReferanse
import SlaaSammenToRegler

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
    slaasammenFunksjon: (C, D) -> S
) = SlaaSammenToRegler(
    versjon = versjon,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regel1 = regel1,
    regel2 = regel2,
    slaasammenFunksjon = slaasammenFunksjon
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
infix fun <G, A, B> Pair<RegelMeta, Regel<G, A>>.og(regel2: Regel<G, B>) = this to regel2
infix fun <G, A : Any, B : Any, S> Pair<Pair<RegelMeta, Regel<G, A>>, Regel<G, B>>.med(f: (A, B) -> S) =
    slaaSammenToRegler(
        first.first.versjon,
        first.first.beskrivelse,
        first.first.regelReferanse,
        first.second,
        second,
        f
    )