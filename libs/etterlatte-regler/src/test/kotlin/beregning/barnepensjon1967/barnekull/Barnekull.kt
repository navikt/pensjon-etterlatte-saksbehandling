package beregning.barnepensjon1967.barnekull

import Regel
import ToDoRegelReferanse
import beregning.barnepensjon1967.Barnepensjon1967Grunnlag
import regler.RegelMeta
import regler.definerKonstant
import regler.finnFaktumIGrunnlag
import regler.kombinerer
import regler.med
import regler.multipliser
import regler.og
import java.math.BigDecimal

private val grunnbeloep: Regel<Barnepensjon1967Grunnlag, BigDecimal> =
    finnFaktumIGrunnlag(
        versjon = "1",
        beskrivelse = "Finner grunnbeløp",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::grunnbeloep,
        finnFelt = { it }
    )

private val antallSoeskenIKullet: Regel<Barnepensjon1967Grunnlag, Int> =
    finnFaktumIGrunnlag(
        versjon = "1",
        beskrivelse = "Finner antall barn i kullet",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::antallSoeskenIKullet,
        finnFelt = { it }
    )

val prosentsatsFoersteBarnKonstant = definerKonstant<Barnepensjon1967Grunnlag, BigDecimal>(
    versjon = "1",
    beskrivelse = "Prosentsats benyttet for første barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = BigDecimal(0.4)
)

val prosentsatsEtterfoelgendeBarnKonstant = definerKonstant<Barnepensjon1967Grunnlag, BigDecimal>(
    versjon = "1",
    beskrivelse = "Prosentsats benyttet for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = BigDecimal(0.25)
)

private val belopForFoersteBarn = RegelMeta(
    versjon = "1",
    beskrivelse = "Satser i kr av for første barn",
    regelReferanse = ToDoRegelReferanse()
) multipliser prosentsatsFoersteBarnKonstant med grunnbeloep

private val belopForEtterfoelgendeBarn = RegelMeta(
    versjon = "1",
    beskrivelse = "Satser i kr av for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse()
) multipliser prosentsatsEtterfoelgendeBarnKonstant med grunnbeloep

private val satser = RegelMeta(
    versjon = "1",
    beskrivelse = "Satser i kr for barn",
    regelReferanse = ToDoRegelReferanse()
) kombinerer belopForFoersteBarn og belopForEtterfoelgendeBarn med { forste, etterfolgende ->
    forste to etterfolgende
}

val barnekullRegel = RegelMeta(
    versjon = "1",
    beskrivelse = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet",
    regelReferanse = ToDoRegelReferanse()
) kombinerer satser og antallSoeskenIKullet med { (foerstebarnSats, etterfoelgendeBarnSats), antallSoesken ->
    (foerstebarnSats + (etterfoelgendeBarnSats * antallSoesken.toBigDecimal())) / (antallSoesken + 1).toBigDecimal()
}