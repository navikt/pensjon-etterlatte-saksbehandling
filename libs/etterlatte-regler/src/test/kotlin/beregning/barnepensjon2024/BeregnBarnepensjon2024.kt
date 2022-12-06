package beregning.barnepensjon2024

import regler.ToDoRegelReferanse
import beregning.BarnepensjonGrunnlag
import beregning.barnepensjon1967.BP_1967_DATO
import beregning.barnepensjon1967.kroneavrundingKonstant
import beregning.barnepensjon1967.trygdetidsfaktor.trygdetidsFaktor
import regler.RegelMeta
import regler.definerKonstant
import regler.kombinerer
import regler.med
import regler.og
import java.math.BigDecimal
import java.time.LocalDate

val BP_2024_DATO: LocalDate = LocalDate.of(2024, 1, 1)

val beloepEnForelderDoed = definerKonstant<BarnepensjonGrunnlag, BigDecimal>(
    gjelderFra = BP_2024_DATO,
    beskrivelse = "Kronebelop benyttet for barn med en forelder død",
    regelReferanse = ToDoRegelReferanse(),
    verdi = BigDecimal(100_000)
)

val reduksjonMotFolketrygdRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse()
) kombinerer beloepEnForelderDoed og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    (sats * trygdetidsfaktor)
}

val beregnBarnepensjon2024Regel = RegelMeta(
    gjelderFra = BP_2024_DATO,
    beskrivelse = "Beregner barnepensjon med regelverk fra 2024",
    regelReferanse = ToDoRegelReferanse()
) kombinerer reduksjonMotFolketrygdRegel og kroneavrundingKonstant med { sum, avrunding ->
    sum.setScale(0, avrunding).toInt()
}