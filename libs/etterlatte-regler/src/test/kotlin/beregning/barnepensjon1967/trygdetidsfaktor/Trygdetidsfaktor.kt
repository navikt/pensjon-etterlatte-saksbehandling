package beregning.barnepensjon1967.trygdetidsfaktor

import Regel
import ToDoRegelReferanse
import beregning.barnepensjon1967.AvdoedForelder
import beregning.barnepensjon1967.Barnepensjon1967Grunnlag
import regler.RegelMeta
import regler.definerKonstant
import regler.finnFaktumIGrunnlag
import regler.kombinerer
import regler.med
import regler.og
import java.math.BigDecimal

private val trygdetidRegel: Regel<Barnepensjon1967Grunnlag, BigDecimal> =
    finnFaktumIGrunnlag(
        versjon = "1",
        beskrivelse = "Finner avdødes trygdetid",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::avdoedForelder,
        finnFelt = AvdoedForelder::trygdetid
    )

private val maksTrygdetid = definerKonstant<Barnepensjon1967Grunnlag, BigDecimal>(
    versjon = "1",
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = ToDoRegelReferanse(),
    verdi = BigDecimal(40)
)

val trygdetidsFaktor = RegelMeta("1", "Finn trygdetidsfaktor", ToDoRegelReferanse()) kombinerer
    maksTrygdetid og trygdetidRegel med { maksTrygdetid, trygdetid ->
    minOf(trygdetid, maksTrygdetid) / maksTrygdetid
}