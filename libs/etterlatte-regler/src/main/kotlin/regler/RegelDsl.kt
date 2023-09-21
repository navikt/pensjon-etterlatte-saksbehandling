package no.nav.etterlatte.libs.regler

import java.time.LocalDate

data class RegelMeta(
    val gjelderFra: LocalDate,
    val beskrivelse: String,
    val regelReferanse: RegelReferanse,
)

private fun <G, S, S1, R : Regel<G, S1>> transformerEnRegel(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel: R,
    transformer: (S1) -> S,
) = TransformerEnRegel(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regel = regel,
    transformer = transformer,
)

private fun <G, S, S1, S2, R1 : Regel<G, S1>, R2 : Regel<G, S2>> transformerToRegler(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel1: R1,
    regel2: R2,
    transformer: (S1, S2) -> S,
) = TransformerToRegler(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regel1 = regel1,
    regel2 = regel2,
    transformer = transformer,
)

private fun <G, S, S1, S2, S3, R1 : Regel<G, S1>, R2 : Regel<G, S2>, R3 : Regel<G, S3>> transformerTreRegler(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regel1: R1,
    regel2: R2,
    regel3: R3,
    transformer: (S1, S2, S3) -> S,
) = TransformerTreRegler(
    gjelderFra = gjelderFra,
    beskrivelse = beskrivelse,
    regelReferanse = regelReferanse,
    regel1 = regel1,
    regel2 = regel2,
    regel3 = regel3,
    transformer = transformer,
)

private fun <G, S> velgNyesteRegel(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    regler: List<Regel<G, S>>,
): Regel<G, S> =
    VelgNyesteGyldigRegel(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = regelReferanse,
        regler = regler,
    )

fun <G, T, S> finnFaktumIGrunnlag(
    gjelderFra: LocalDate,
    beskrivelse: String,
    finnFaktum: (G) -> FaktumNode<T>,
    finnFelt: (T) -> S,
): Regel<G, S> =
    FinnFaktumIGrunnlagRegel(
        gjelderFra,
        beskrivelse,
        finnFaktum,
        finnFelt,
    )

fun <G, S> definerKonstant(
    gjelderFra: LocalDate,
    beskrivelse: String,
    regelReferanse: RegelReferanse,
    verdi: S,
): Regel<G, S> =
    KonstantRegel(
        gjelderFra,
        beskrivelse,
        regelReferanse,
        verdi,
    )

infix fun <G, S> RegelMeta.benytter(regel1: Regel<G, S>) = this to regel1

infix fun <G, S> RegelMeta.velgNyesteGyldige(regler: List<Regel<G, S>>) =
    velgNyesteRegel(
        gjelderFra = gjelderFra,
        beskrivelse = beskrivelse,
        regelReferanse = regelReferanse,
        regler = regler,
    )

infix fun <G, S1, S2> Pair<RegelMeta, Regel<G, S1>>.og(regel2: Regel<G, S2>) = this to regel2

infix fun <G, S1, S2, S3> Pair<Pair<RegelMeta, Regel<G, S1>>, Regel<G, S2>>.og(third: Regel<G, S3>) = Triple(first, second, third)

infix fun <G, S> Regel<G, S>.og(that: Regel<G, S>) = listOf(this, that)

infix fun <G, S> List<Regel<G, S>>.og(that: Regel<G, S>) = this.plus(that)

infix fun <G, S, S1> Pair<RegelMeta, Regel<G, S1>>.med(transformer: (S1) -> S) =
    transformerEnRegel(
        gjelderFra = first.gjelderFra,
        beskrivelse = first.beskrivelse,
        regelReferanse = first.regelReferanse,
        regel = second,
        transformer = transformer,
    )

infix fun <G, S1, S2, S> Pair<Pair<RegelMeta, Regel<G, S1>>, Regel<G, S2>>.med(transformer: (S1, S2) -> S) =
    transformerToRegler(
        gjelderFra = first.first.gjelderFra,
        beskrivelse = first.first.beskrivelse,
        regelReferanse = first.first.regelReferanse,
        regel1 = first.second,
        regel2 = second,
        transformer = transformer,
    )

infix fun <G, S1, S2, S3, S> Triple<Pair<RegelMeta, Regel<G, S1>>, Regel<G, S2>, Regel<G, S3>>.med(transformer: (S1, S2, S3) -> S) =
    transformerTreRegler(
        gjelderFra = first.first.gjelderFra,
        beskrivelse = first.first.beskrivelse,
        regelReferanse = first.first.regelReferanse,
        regel1 = first.second,
        regel2 = second,
        regel3 = third,
        transformer = transformer,
    )
