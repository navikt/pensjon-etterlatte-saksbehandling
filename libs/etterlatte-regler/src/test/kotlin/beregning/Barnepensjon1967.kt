package beregning.barnepensjon1967

import FaktumNode
import KonstantNode
import Regel
import RegelVisitor
import SubsumsjonsNode
import ToDoRegelReferanse
import regler.RegelMeta
import regler.definerKonstant
import regler.finnFaktumIGrunnlag
import regler.kombinerer
import regler.med
import regler.og
import java.math.BigDecimal
import java.math.RoundingMode

data class Barnepensjon1967Grunnlag(
    val grunnbeloep: FaktumNode<Long>,
    val antallSoeskenIKullet: FaktumNode<Int>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
)

data class AvdoedForelder(val trygdetid: Int)

fun reduksjonMotrygdetidFormel(barnekull: Double, trygdetid: Pair<Int, Int>) =
    BigDecimal(barnekull * trygdetid.second / trygdetid.first)
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()

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

val reduksjonMotFolketrygdRegel = RegelMeta(
    versjon = "1",
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse()
) kombinerer BarnekullRegel og trygdetidsFaktor med ::reduksjonMotrygdetidFormel

object BarnekullRegel : Regel<Barnepensjon1967Grunnlag, Double> {
    override val versjon: String = "1"
    override val beskrivelse: String = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet"
    override val regelReferanse = ToDoRegelReferanse()

    override fun accept(visitor: RegelVisitor) {
        TODO("Not yet implemented")
    }

    private val PROSENTSATS_FOERSTE_BARN =
        KonstantNode(0.4, "Prosentsats benyttet for første barn")
    private val PROSENTSATS_ETTERFOELGENDE_BARN =
        KonstantNode(0.25, "Prosentsats benyttet for etterfølgende barn")

    override fun anvend(grunnlag: Barnepensjon1967Grunnlag): SubsumsjonsNode<Double> {
        val foersteBarn = grunnlag.grunnbeloep.verdi * PROSENTSATS_FOERSTE_BARN.verdi
        val etterfoelgendeBarn =
            grunnlag.grunnbeloep.verdi * PROSENTSATS_ETTERFOELGENDE_BARN.verdi * grunnlag.antallSoeskenIKullet.verdi

        /** 40% av G til første barn, 25% til resten. Fordeles likt */
        return SubsumsjonsNode(
            verdi = (foersteBarn + etterfoelgendeBarn) / grunnlag.antallSoeskenIKullet.verdi.plus(1),
            regel = this,
            children = listOf(
                grunnlag.grunnbeloep,
                grunnlag.antallSoeskenIKullet,
                PROSENTSATS_FOERSTE_BARN,
                PROSENTSATS_ETTERFOELGENDE_BARN
            )
        )
    }
}