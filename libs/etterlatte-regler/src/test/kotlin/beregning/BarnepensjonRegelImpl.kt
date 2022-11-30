package beregning

import FaktumNode
import Regel
import RegelNode
import RegelVisitor
import SlaaSammenToRegler
import ToDoRegelReferanse
import beregning.ReduksjonMotFolketrygdRegel.MAX_TRYGDETID
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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

object ReduksjonMotFolketrygdRegel :
    SlaaSammenToRegler<
            Double,
            Int,
            Barnepensjon1967Grunnlag,
            Int,
            Regel<Barnepensjon1967Grunnlag, Double>,
            Regel<Barnepensjon1967Grunnlag, Int>>(
        versjon = "1",
        beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
        regelReferanse = ToDoRegelReferanse(),
        regel1 = BarnekullRegel,
        regel2 = TrygdetidRegel,
        slaasammenFunksjon = { barnekull, trygdetid ->
            val resultat = BigDecimal(barnekull * trygdetid / MAX_TRYGDETID.verdi)
            resultat.setScale(0, RoundingMode.HALF_UP).toInt()
        }
    ) {

    private val MAX_TRYGDETID = FaktumNode(40, regelKilde, "Maks antall års trygdetid")
}

object BarnekullRegel : Regel<Barnepensjon1967Grunnlag, Double> {
    override val versjon: String = "1"
    override val beskrivelse: String = "Beregn uavkortet barnepensjon basert på størrelsen på barnekullet"
    override val regelReferanse = ToDoRegelReferanse()

    override fun visited(visitor: RegelVisitor) {
        TODO("Not yet implemented")
    }

    private val PROSENTSATS_FOERSTE_BARN = FaktumNode(0.4, regelKilde, "Prosentsats benyttet for første barn")
    private val PROSENTSATS_ETTERFOELGENDE_BARN =
        FaktumNode(0.25, regelKilde, "Prosentsats benyttet for etterfølgende barn")

    override fun anvend(grunnlag: Barnepensjon1967Grunnlag): RegelNode<Double> {
        val foersteBarn = grunnlag.grunnbeloep.verdi * PROSENTSATS_FOERSTE_BARN.verdi
        val etterfoelgendeBarn =
            grunnlag.grunnbeloep.verdi * PROSENTSATS_ETTERFOELGENDE_BARN.verdi * grunnlag.antallSoeskenIKullet.verdi

        /** 40% av G til første barn, 25% til resten. Fordeles likt */
        return RegelNode(
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

    override fun visited(visitor: RegelVisitor) {
        TODO("Not yet implemented")
    }

    override fun anvend(grunnlag: Barnepensjon1967Grunnlag): RegelNode<Int> {
        return RegelNode(
            verdi = grunnlag.avdoedForelder.verdi.trygdetid,
            regel = this,
            children = listOf(grunnlag.avdoedForelder)
        )
    }
}