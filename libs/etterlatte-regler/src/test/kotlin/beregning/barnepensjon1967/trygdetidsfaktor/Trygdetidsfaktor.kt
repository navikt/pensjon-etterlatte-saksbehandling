package beregning.barnepensjon1967.trygdetidsfaktor

import Regel
import ToDoRegelReferanse
import beregning.AvdoedForelder
import beregning.Barnepensjon1967Grunnlag
import beregning.barnepensjon1967.BP_1967_DATO
import regler.RegelMeta
import regler.definerKonstant
import regler.finnFaktumIGrunnlag
import regler.kombinerer
import regler.med
import regler.og
import java.math.BigDecimal

private val trygdetidRegel: Regel<Barnepensjon1967Grunnlag, BigDecimal> =
    finnFaktumIGrunnlag(
        gjelderFra = BP_1967_DATO,
        beskrivelse = "Finner avdødes trygdetid",
        regelReferanse = ToDoRegelReferanse(),
        finnFaktum = Barnepensjon1967Grunnlag::avdoedForelder,
        finnFelt = AvdoedForelder::trygdetid
    )

private val maksTrygdetid = definerKonstant<Barnepensjon1967Grunnlag, BigDecimal>(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = ToDoRegelReferanse(),
    verdi = BigDecimal(40)
)

val trygdetidsFaktor = RegelMeta(gjelderFra = BP_1967_DATO, "Finn trygdetidsfaktor", ToDoRegelReferanse()) kombinerer
    maksTrygdetid og trygdetidRegel med { maksTrygdetid, trygdetid ->
    minOf(trygdetid, maksTrygdetid) / maksTrygdetid
}