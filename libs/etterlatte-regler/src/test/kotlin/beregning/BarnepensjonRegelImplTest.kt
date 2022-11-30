package beregning

import FaktumNode
import finnAlleRegler
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.Test
import java.time.Instant

class BarnepensjonRegelImplTest {
    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())

    @Test
    fun `Skal justere for soesken og redusere barnepensjon mot folketrygd`() {
        val grunnlag = Barnepensjon1967Grunnlag(
            grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = 100_550L),
            antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
            avdoedForelder = FaktumNode(
                kilde = saksbehandler,
                beskrivelse = "Avdød forelders trygdetid",
                verdi = AvdoedForelder(trygdetid = 30)
            )
        )

        val beregning = ReduksjonMotFolketrygdRegel.anvend(grunnlag)

        beregning.verdi shouldBe 22_624
        beregning.regel should beInstanceOf<ReduksjonMotFolketrygdRegel>()
        beregning.children.size shouldBe 3

//        when (val node = beregning.children[0]) {
//            is RegelNode<*, *> -> {
//                node.verdi shouldBe 30_165.0
//                node.regel should beInstanceOf<beregning.BarnekullRegel>()
//                node.children shouldHaveSize 4
//            }
//            else -> throw Exception("Node har feil type")
//        }
//
//        when (val node = beregning.children[1]) {
//            is RegelNode<*, *> -> {
//                node.verdi shouldBe 30
//                node.regel should beInstanceOf<beregning.TrygdetidRegel>()
//                node.children shouldHaveSize 1
//            }
//            else -> throw Exception("Node har feil type")
//        }
//
//        when (val node = beregning.children[2]) {
//            is FaktumNode -> {
//                node.verdi shouldBe 40
//                node.kilde should beInstanceOf<Grunnlagsopplysning.RegelKilde>()
//            }
//            else -> throw Exception("Node har feil type")
//        }

        println(beregning.toJsonNode().toPrettyString())

        val finnAlleRegler = beregning.finnAlleRegler()

        println(finnAlleRegler)
    }

    @Test
    fun `Alenebarn uten avkorting`() {
        val grunnlag = Barnepensjon1967Grunnlag(
            grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = 100_000L),
            antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 0),
            avdoedForelder = FaktumNode(
                kilde = saksbehandler,
                beskrivelse = "Avdød forelders trygdetid",
                verdi = AvdoedForelder(trygdetid = 40)
            )
        )

        val beregning = ReduksjonMotFolketrygdRegel.anvend(grunnlag)

        beregning.verdi shouldBe 40_000
        beregning.regel should beInstanceOf<ReduksjonMotFolketrygdRegel>()
        beregning.children.size shouldBe 3

//        when (val node = beregning.children[0]) {
//            is RegelNode<*, *> -> {
//                node.verdi shouldBe 40000.0
//                node.regel should beInstanceOf<beregning.BarnekullRegel>()
//                node.children shouldHaveSize 4
//            }
//            else -> throw Exception("Node har feil type")
//        }
//
//        when (val node = beregning.children[1]) {
//            is RegelNode<*, *> -> {
//                node.verdi shouldBe 40
//                node.regel should beInstanceOf<beregning.TrygdetidRegel>()
//                node.children shouldHaveSize 1
//            }
//            else -> throw Exception("Node har feil type")
//        }
//
//        when (val node = beregning.children[2]) {
//            is FaktumNode -> {
//                node should beInstanceOf<FaktumNode<Int>>()
//                node.verdi shouldBe 40
//                node.kilde should beInstanceOf<Grunnlagsopplysning.RegelKilde>()
//            }
//            else -> throw Exception("Node har feil type")
//        }

        println(beregning.toJsonNode().toPrettyString())
    }
}