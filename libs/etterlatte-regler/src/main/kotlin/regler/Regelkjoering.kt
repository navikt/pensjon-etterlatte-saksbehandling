package regler

import java.time.LocalDate

sealed class RegelkjoeringResultat<S> {
    data class Suksess<S>(val resultat: Map<RegelPeriode, SubsumsjonsNode<S>>) : RegelkjoeringResultat<S>()
    data class UgyldigPeriode<S>(val ugyldigePerioder: List<Regel<*, *>>) : RegelkjoeringResultat<S>()
}

object Regelkjoering {
    fun <G, S> eksekver(
        regel: Regel<G, S>,
        grunnlag: G,
        periode: RegelPeriode
    ): RegelkjoeringResultat<S> {
        val ugyldigePerioder = regel.finnUgyldigePerioder(periode)

        return if (ugyldigePerioder.isEmpty()) {
            val knekkpunkterFraRegler = regel.finnAlleKnekkpunkter()
                .filter { knekkpunktGyldigForPeriode(it, periode) }.toSet()
            val knekkpunkterFraPeriode = setOf(periode.fraDato, periode.tilDato).filterNotNull()
            val knekkpunkter = knekkpunkterFraRegler.plus(knekkpunkterFraPeriode).sorted().toSet()
            // TODO dette bør løses mer elegant
            val perioder =
                if (periode.tilDato == null) {
                    listOf(periode)
                } else {
                    knekkpunkter.zipWithNext { a, b -> RegelPeriode(a, b) }
                }

            val periodisertResultat = perioder.associateWith { p -> regel.anvend(grunnlag, p) }
            RegelkjoeringResultat.Suksess(periodisertResultat)
        } else {
            RegelkjoeringResultat.UgyldigPeriode(ugyldigePerioder)
        }
    }

    private fun knekkpunktGyldigForPeriode(it: LocalDate, periode: RegelPeriode) =
        if (periode.tilDato == null) {
            it == periode.fraDato
        } else {
            it >= periode.fraDato && it <= periode.tilDato
        }
}

fun <G, S> Regel<G, S>.eksekver(grunnlag: G, periode: RegelPeriode) = Regelkjoering.eksekver(
    regel = this,
    grunnlag = grunnlag,
    periode = periode
)