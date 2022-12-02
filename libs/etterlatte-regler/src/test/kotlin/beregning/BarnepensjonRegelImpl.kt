package beregning

import FaktumNode
import KonstantNode
import Regel
import RegelVisitor
import SlaaSammenToRegler
import SubsumsjonsNode
import ToDoRegelReferanse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import regler.RegelMeta
import regler.kombinerer
import regler.med
import regler.og
import regler.slaaSammenToRegler
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

private val regelKilde = Grunnlagsopplysning.RegelKilde("EtterlatteRegler", Instant.now(), "1")

data class AvdoedForelder(val trygdetid: Int)
data class Barnepensjon1967Grunnlag(
    val grunnbeloep: FaktumNode<Long>,
    val antallSoeskenIKullet: FaktumNode<Int>,
    val avdoedForelder: FaktumNode<AvdoedForelder>
)

fun reduksjonMotrygdetidFormel(barnekull: Double, trygdetid: Pair<Int, Int>) =
    BigDecimal(barnekull * trygdetid.second / trygdetid.first)
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()

val trygdetidsFaktor = RegelMeta("1", "Finn trygdetidsbrøk", ToDoRegelReferanse()) kombinerer
    MaksTrygdetidConstant og TrygdetidRegel med
    { maksTrygdetid, trygdetid ->
        maksTrygdetid to minOf(trygdetid, maksTrygdetid)
    }

val reduksjonMotFolketrygdRegel2 = RegelMeta(
    versjon = "1",
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse()
) kombinerer BarnekullRegel og TrygdetidsFaktor med ::reduksjonMotrygdetidFormel

val reduksjonMotFolketrygdRegel = slaaSammenToRegler(
    versjon = "1",
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = ToDoRegelReferanse(),
    regel1 = BarnekullRegel,
    regel2 = TrygdetidsFaktor
) { barnekull, trygdetid ->
    val resultat = BigDecimal(barnekull * trygdetid.second / trygdetid.first)
    resultat.setScale(0, RoundingMode.HALF_UP).toInt()
}

object ReduksjonMotFolketrygdRegel :
    SlaaSammenToRegler<
        Double,
        Pair<Int, Int>,
        Barnepensjon1967Grunnlag,
        Int,
        Regel<Barnepensjon1967Grunnlag, Double>,
        Regel<Barnepensjon1967Grunnlag, Pair<Int, Int>>>(
        versjon = "1",
        beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
        regelReferanse = ToDoRegelReferanse(),
        venstre = BarnekullRegel,
        hoeyre = TrygdetidsFaktor,
        slaasammenFunksjon = { barnekull, trygdetid ->
            val resultat = BigDecimal(barnekull * trygdetid.second / trygdetid.first)
            resultat.setScale(0, RoundingMode.HALF_UP).toInt()
        }
    )

object TrygdetidsFaktor :
    SlaaSammenToRegler<
        Int,
        Int,
        Barnepensjon1967Grunnlag,
        Pair<Int, Int>,
        Regel<Barnepensjon1967Grunnlag, Int>,
        Regel<Barnepensjon1967Grunnlag, Int>>(
        versjon = "1",
        beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
        regelReferanse = ToDoRegelReferanse(),
        venstre = MaksTrygdetidConstant,
        hoeyre = TrygdetidRegel,
        slaasammenFunksjon = { maksTrygdetid, trygdetid ->
            maksTrygdetid to minOf(trygdetid, maksTrygdetid)
        }
    )

object MaksTrygdetidConstant : Regel<Barnepensjon1967Grunnlag, Int> {
    override val versjon: String = "1"
    override val beskrivelse: String = "Full trygdetidsopptjening er 40 år"
    override val regelReferanse = ToDoRegelReferanse()

    override fun accept(visitor: RegelVisitor) {
        TODO("Not yet implemented")
    }

    override fun anvend(grunnlag: Barnepensjon1967Grunnlag): SubsumsjonsNode<Int> = SubsumsjonsNode(
        verdi = 40,
        regel = this,
        children = listOf(
            KonstantNode(40, "Maks antall års trygdetid")
        )
    )
}

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

object TrygdetidRegel : Regel<Barnepensjon1967Grunnlag, Int> {
    override val versjon: String = "1"
    override val beskrivelse: String = "Finner avdødes trygdetid"
    override val regelReferanse = ToDoRegelReferanse()

    override fun accept(visitor: RegelVisitor) {
        TODO("Not yet implemented")
    }

    override fun anvend(grunnlag: Barnepensjon1967Grunnlag): SubsumsjonsNode<Int> {
        return SubsumsjonsNode(
            verdi = grunnlag.avdoedForelder.verdi.trygdetid,
            regel = this,
            children = listOf(grunnlag.avdoedForelder)
        )
    }
}