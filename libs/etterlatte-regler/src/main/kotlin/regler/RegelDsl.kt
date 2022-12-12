package regler

import java.math.BigDecimal
import java.time.LocalDate

data class RegelMeta(
    val gjelderFra: LocalDate,
    val beskrivelse: String,
    val regelReferanse: RegelReferanse
)

fun <C : Any, D : Any, G, S, A : Regel<G, C>, B : Regel<G, D>> slaaSammenToRegler(
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
    regel1 = regel1,
    regel2 = regel2,
    slaasammenFunksjon = slaaSammenFunksjon
)

fun <D : Any, E : Any, F : Any, G, S, A : Regel<G, D>, B : Regel<G, E>, C : Regel<G, F>>
slaaSammenTreRegler(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel1: A,
    regel2: B,
    regel3: C,
    slaaSammenFunksjon: (D, E, F) -> S
) = SlaaSammenTreRegler(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regel1 = regel1,
    regel2 = regel2,
    regel3 = regel3,
    slaasammenFunksjon = slaaSammenFunksjon
)

fun <G, S : Any> velgNyesteRegel(
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

fun <G> gangSammenRegler(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regler: List<Regel<G, BigDecimal>>
): Regel<G, BigDecimal> = GangSammenRegel(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regler = regler
)

fun <G, T : Any, A : FaktumNode<T>, S> finnFaktumIGrunnlag(
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

fun <G, S> definerKonstant(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    verdi: S
): Regel<G, S> = KonstantRegel(
    gjelderFra,
    beskrivelse,
    regelReferanse,
    verdi
)

infix fun <G, A> RegelMeta.kombinerer(regel1: Regel<G, A>) = this to regel1
infix fun <G> RegelMeta.multipliser(regler: List<Regel<G, BigDecimal>>) = gangSammenRegler(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regler = regler
)

infix fun <G, A : Any> RegelMeta.velgNyesteGyldige(regler: List<Regel<G, A>>) = velgNyesteRegel(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regler = regler
)

infix fun <G, A, B> Pair<RegelMeta, Regel<G, A>>.og(regel2: Regel<G, B>) = this to regel2
infix fun <G, A, B, C> Pair<Pair<RegelMeta, Regel<G, A>>, Regel<G, B>>.og(third: Regel<G, C>) =
    Triple(first, second, third)

infix fun <G, A : Any> Regel<G, A>.og(that: Regel<G, A>) = listOf(this, that)
infix fun <G, A : Any> List<Regel<G, A>>.og(that: Regel<G, A>) = this.plus(that)
infix fun <G, A : Any, B : Any, S> Pair<Pair<RegelMeta, Regel<G, A>>, Regel<G, B>>.med(
    f: (A, B) -> S
) = slaaSammenToRegler(
    gjelderFra = first.first.gjelderFra,
    beskrivelse = first.first.beskrivelse,
    regelReferanse = first.first.regelReferanse,
    regel1 = first.second,
    regel2 = second,
    slaaSammenFunksjon = f
)

infix fun <G, A : Any, B : Any, C : Any, S>
Triple<Pair<RegelMeta, Regel<G, A>>, Regel<G, B>, Regel<G, C>>.med(
    f: (A, B, C) -> S
) = slaaSammenTreRegler(
    gjelderFra = first.first.gjelderFra,
    beskrivelse = first.first.beskrivelse,
    regelReferanse = first.first.regelReferanse,
    regel1 = first.second,
    regel2 = second,
    regel3 = third,
    slaaSammenFunksjon = f
)