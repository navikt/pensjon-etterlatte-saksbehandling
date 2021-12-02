package no.nav.etterlatte.beregning

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.math.BigDecimal
import java.math.RoundingMode


fun Route.beregningRoutes(){
    post("/beregnAp") {
        call.respond(beregnAlderspensjonKapittel3(call.receive()))
    }
}

fun beregnAlderspensjonKapittel3(grunnlag: BeregningsGrunnlag) =tillegspensjon(grunnlag.opptjening, grunnlag.G).let { tilleggspensjon -> Beregning(grunnpensjon(grunnlag.G), tilleggspensjon, særtillegg(grunnlag.G, tilleggspensjon)) }

data class BeregningsGrunnlag(val G: Int, val opptjening: List<PoengÅr>)

data class Beregning(val grunnpensjon: BigDecimal, val tilleggspensjon: BigDecimal, val særtillegg: BigDecimal)
fun tillegspensjon(opptjening:List<PoengÅr>, grunnbeløp: Int): BigDecimal {
    if(opptjening.size < 5) return BigDecimal.ZERO
    return ((grunnbeløp.toBigDecimal()  *  sluttPoengtall(opptjening) * (opptjening.sortedBy { it.år }.take(40).map { if(it.år > 1991) BigDecimal(0.42) else BigDecimal(0.45) }.reduce{acc, it ->acc + it})) /40.toBigDecimal()).setScale(4, RoundingMode.HALF_UP)
}

fun sluttPoengtall(opptjening:List<PoengÅr>) = opptjening.sortedBy { it.poeng }.takeLast(20).let { it.sumOf { it.poeng }/it.size.toBigDecimal() }
fun grunnpensjon(grunnbeløp: Int) = grunnbeløp.toBigDecimal()
fun særtillegg(grunnbeløp: Int, tilleggspensjon: BigDecimal) = maxOf(BigDecimal.ZERO, grunnbeløp.toBigDecimal() - tilleggspensjon)


class PoengÅr(val år:Int, val poeng: BigDecimal)