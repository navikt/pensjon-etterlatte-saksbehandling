package beregning.barnepensjon1967

import ToDoRegelReferanse
import beregning.barnepensjon1967.barnekull.barnekullRegel
import beregning.barnepensjon1967.trygdetidsfaktor.trygdetidsFaktor
import regler.RegelMeta
import regler.definerKonstant
import regler.kombinerer
import regler.med
import regler.og
import java.math.RoundingMode

val reduksjonMotFolketrygdRegel2 = RegelMeta(
    versjon = "1",
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse()
) kombinerer barnekullRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    (sats * trygdetidsfaktor)
}

val kroneavrundingKonstant = definerKonstant<Barnepensjon1967Grunnlag, RoundingMode>(
    versjon = "1",
    beskrivelse = """
        Formel for avrunding til nærmeste krone. Dersom det er like langt, rund opp.
    """.trimIndent(),
    regelReferanse = ToDoRegelReferanse(),
    verdi = RoundingMode.HALF_UP
)

val beregnBarnepensjon1967Regel = RegelMeta(
    versjon = "1",
    beskrivelse = "Beregner barnepensjon med regelverk fra 1967",
    regelReferanse = ToDoRegelReferanse()
) kombinerer reduksjonMotFolketrygdRegel2 og kroneavrundingKonstant med { sum, avrunding ->
    sum.setScale(0, avrunding).toInt()
}