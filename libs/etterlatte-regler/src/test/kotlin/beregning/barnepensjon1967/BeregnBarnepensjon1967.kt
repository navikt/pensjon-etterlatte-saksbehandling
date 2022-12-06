package beregning.barnepensjon1967

import ToDoRegelReferanse
import beregning.BarnepensjonGrunnlag
import beregning.barnepensjon1967.barnekull.barnekullRegel
import beregning.barnepensjon1967.trygdetidsfaktor.trygdetidsFaktor
import regler.RegelMeta
import regler.definerKonstant
import regler.kombinerer
import regler.med
import regler.og
import java.math.RoundingMode
import java.time.LocalDate

val BP_1967_DATO: LocalDate = LocalDate.of(1967, 1, 1)

val reduksjonMotFolketrygdRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse()
) kombinerer barnekullRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    (sats * trygdetidsfaktor)
}

val kroneavrundingKonstant = definerKonstant<BarnepensjonGrunnlag, RoundingMode>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = """
        Formel for avrunding til nærmeste krone. Dersom det er like langt, rund opp.
    """.trimIndent(),
    regelReferanse = ToDoRegelReferanse(),
    verdi = RoundingMode.HALF_UP
)

val beregnBarnepensjon1967Regel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Beregner barnepensjon med regelverk fra 1967",
    regelReferanse = ToDoRegelReferanse()
) kombinerer reduksjonMotFolketrygdRegel og kroneavrundingKonstant med { sum, avrunding ->
    sum.setScale(0, avrunding).toInt()
}