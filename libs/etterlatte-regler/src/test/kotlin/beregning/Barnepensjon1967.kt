package beregning.barnepensjon1967

import FaktumNode
import Regel
import ToDoRegelReferanse
import regler.RegelMeta
import regler.definerKonstant
import regler.finnFaktumIGrunnlag
import regler.gangSammenToRegler
import regler.kombinerer
import regler.med
import regler.og
import java.math.BigDecimal
import java.math.RoundingMode

data class AvdoedForelder(val trygdetid: Int)
data class Barnepensjon1967Grunnlag(
    val grunnbeloep: FaktumNode<Long>,
    val antallSoeskenIKullet: FaktumNode<Int>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
)

val trygdetidRegel: Regel<Barnepensjon1967Grunnlag, Int> =
    finnFaktumIGrunnlag(
        versjon = "1",
        beskrivelse = "Finner avdødes trygdetid",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::avdoedForelder,
        finnFelt = AvdoedForelder::trygdetid
    )

val maksTrygdetid = definerKonstant<Barnepensjon1967Grunnlag, Int>(
    versjon = "1",
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = ToDoRegelReferanse(),
    verdi = 40
)

val prosentsatsFoersteBarnKonstant = definerKonstant<Barnepensjon1967Grunnlag, Double>(
    versjon = "1",
    beskrivelse = "Prosentsats benyttet for første barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = 0.4
)

val prosentsatsEtterfoelgendeBarnKonstant = definerKonstant<Barnepensjon1967Grunnlag, Double>(
    versjon = "1",
    beskrivelse = "Prosentsats benyttet for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse(),
    verdi = 0.25
)

val trygdetidsFaktor = RegelMeta("1", "Finn trygdetidsbrøk", ToDoRegelReferanse()) kombinerer
    maksTrygdetid og trygdetidRegel med { maksTrygdetid, trygdetid ->
    maksTrygdetid to minOf(trygdetid, maksTrygdetid)
}

val grunnbeloep: Regel<Barnepensjon1967Grunnlag, Int> =
    finnFaktumIGrunnlag(
        versjon = "1",
        beskrivelse = "Finner grunnbeløp",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::grunnbeloep,
        finnFelt = { it.toInt() }
    )

val belopForFoersteBarn2 = gangSammenToRegler(
    versjon = "1",
    beskrivelse = "Satser i kr av for første barn",
    regelReferanse = ToDoRegelReferanse(),
    regel1 = grunnbeloep,
    regel2 = prosentsatsFoersteBarnKonstant
)

val belopForFoersteBarn = RegelMeta(
    versjon = "1",
    beskrivelse = "Satser i kr av for første barn",
    regelReferanse = ToDoRegelReferanse()
) kombinerer prosentsatsFoersteBarnKonstant og grunnbeloep med { satsIG, G -> satsIG * G }

val belopForEtterfoelgendeBarn = RegelMeta(
    versjon = "1",
    beskrivelse = "Satser i kr av for etterfølgende barn",
    regelReferanse = ToDoRegelReferanse()
) kombinerer prosentsatsEtterfoelgendeBarnKonstant og grunnbeloep med { satsIG, G -> satsIG * G }

val antallSoeskenIKullet: Regel<Barnepensjon1967Grunnlag, Int> =
    finnFaktumIGrunnlag(
        versjon = "1",
        beskrivelse = "Finner antall barn i kullet",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::antallSoeskenIKullet,
        finnFelt = { it }
    )

val satser = RegelMeta(
    versjon = "1",
    beskrivelse = "Satser i kr for barn",
    regelReferanse = ToDoRegelReferanse()
) kombinerer belopForFoersteBarn og belopForEtterfoelgendeBarn med { forste, etterfolgende -> forste to etterfolgende }

val barnekullRegel = RegelMeta(
    versjon = "1",
    beskrivelse = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet",
    regelReferanse = ToDoRegelReferanse()
) kombinerer satser og antallSoeskenIKullet med { (foerstebarn, etterfoelgendeBarn), antallSoesken ->
    (foerstebarn + (etterfoelgendeBarn * antallSoesken)) / (antallSoesken + 1)
}

val reduksjonMotFolketrygdRegel = RegelMeta(
    versjon = "1",
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse()
) kombinerer barnekullRegel og trygdetidsFaktor med { sats, (maksTrygdetid, faktiskTrygdetid) ->
    BigDecimal(sats * faktiskTrygdetid / maksTrygdetid)
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()
}