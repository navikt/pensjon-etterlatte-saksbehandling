package beregning.barnepensjon1967.barnekull

import Regel
import ToDoRegelReferanse
import beregning.Barnepensjon1967Grunnlag
import beregning.barnepensjon1967.BP_1967_DATO
import regler.RegelMeta
import regler.definerKonstant
import regler.finnFaktumIGrunnlag
import regler.kombinerer
import regler.med
import regler.multipliser
import regler.og
import java.math.BigDecimal
import java.time.LocalDate

private val grunnbeloep: Regel<Barnepensjon1967Grunnlag, BigDecimal> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner grunnbeløp",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::grunnbeloep,
        finnFelt = { it }
    )

private val antallSoeskenIKullet: Regel<Barnepensjon1967Grunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner antall barn i kullet",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::antallSoeskenIKullet,
        finnFelt = { it }
    )

val prosentsatsFoersteBarnKonstant = definerKonstant<Barnepensjon1967Grunnlag, BigDecimal>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Prosentsats benyttet for første barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = BigDecimal(0.4)
)

private val belopForFoersteBarn = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Satser i kr av for første barn",
    regelReferanse = ToDoRegelReferanse()
) multipliser prosentsatsFoersteBarnKonstant med grunnbeloep

val prosentsatsEtterfoelgendeBarnKonstant = definerKonstant<Barnepensjon1967Grunnlag, BigDecimal>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Prosentsats benyttet for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = BigDecimal(0.25)
)

private val belopForEtterfoelgendeBarn = RegelMeta(
    gjelderFra = LocalDate.of(1972, 1, 1),
    beskrivelse = "Satser i kr av for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse()
) multipliser prosentsatsEtterfoelgendeBarnKonstant med grunnbeloep

private val satser = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Satser i kr for barn",
    regelReferanse = ToDoRegelReferanse()
) kombinerer belopForFoersteBarn og belopForEtterfoelgendeBarn med { forste, etterfolgende ->
    forste to etterfolgende
}

val barnekullRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet",
    regelReferanse = ToDoRegelReferanse()
) kombinerer satser og antallSoeskenIKullet med { (foerstebarnSats, etterfoelgendeBarnSats), antallSoesken ->
    (foerstebarnSats + (etterfoelgendeBarnSats * antallSoesken.toBigDecimal())) / (antallSoesken + 1).toBigDecimal()
}