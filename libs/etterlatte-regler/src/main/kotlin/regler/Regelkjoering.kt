package regler

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
            val periodisertResultat = regel
                .finnAlleKnekkpunkter()
                .zipWithNext { a, b -> RegelPeriode(a, b) }
                .associateWith { nyPeriode -> regel.anvend(grunnlag, nyPeriode) }

            RegelkjoeringResultat.Suksess(periodisertResultat)
        } else {
            RegelkjoeringResultat.UgyldigPeriode(ugyldigePerioder)
        }
    }
}

fun <G, S> Regel<G, S>.eksekver(grunnlag: G, periode: RegelPeriode) = Regelkjoering.eksekver(
    regel = this,
    grunnlag = grunnlag,
    periode = periode
)