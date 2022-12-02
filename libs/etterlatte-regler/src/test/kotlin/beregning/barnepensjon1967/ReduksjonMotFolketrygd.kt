package beregning.barnepensjon1967

import ToDoRegelReferanse
import beregning.barnepensjon1967.barnekull.barnekullRegel
import beregning.barnepensjon1967.trygdetidsfaktor.trygdetidsFaktor
import regler.RegelMeta
import regler.kombinerer
import regler.med
import regler.og
import java.math.RoundingMode

val reduksjonMotFolketrygdRegel2 = RegelMeta(
    versjon = "1",
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse()
) kombinerer barnekullRegel og trygdetidsFaktor med { sats, (maksTrygdetid, faktiskTrygdetid) ->
    (sats * faktiskTrygdetid / maksTrygdetid)
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()
}