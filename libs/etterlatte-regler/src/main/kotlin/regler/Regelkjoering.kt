package regler

import java.time.LocalDate

sealed class RegelkjoeringResultat<S> {
    data class Suksess<S>(val resultat: Map<RegelPeriode, SubsumsjonsNode<S>>) : RegelkjoeringResultat<S>()
    data class UgyldigPeriode<S>(val ugyldigeReglerForPeriode: List<Regel<*, *>>) : RegelkjoeringResultat<S>()
}

object Regelkjoering {
    fun <G, S> eksekver(
        regel: Regel<G, S>,
        grunnlag: G,
        periode: RegelPeriode
    ): RegelkjoeringResultat<S> {
        val ugyldigePerioder = regel.finnUgyldigePerioder(periode)

        return if (ugyldigePerioder.isEmpty()) {
            regel.finnAlleKnekkpunkter()
                .asSequence()
                .filter { knekkpunktGyldigForPeriode(it, periode) }
                .plus(periode.fraDato)
                .plus(periode.tilDato?.plusDays(1) ?: LocalDate.MAX)
                .sorted()
                .toSet()
                .zipWithNext { periodeFra, nestePeriodeFra ->
                    RegelPeriode(
                        fraDato = periodeFra,
                        tilDato = nestePeriodeFra.takeIf { it.isBefore(LocalDate.MAX) }?.minusDays(1) ?: periode.tilDato
                    )
                }
                .toList()
                .associateWith { p -> regel.anvend(grunnlag, p) }
                .let { RegelkjoeringResultat.Suksess(it) }
        } else {
            RegelkjoeringResultat.UgyldigPeriode(ugyldigePerioder)
        }
    }

    private fun knekkpunktGyldigForPeriode(it: LocalDate, periode: RegelPeriode) =
        it.isAfter(periode.fraDato) && it.isBefore(periode.tilDato ?: LocalDate.MAX)
}

fun <G, S> Regel<G, S>.eksekver(grunnlag: G, periode: RegelPeriode) = Regelkjoering.eksekver(
    regel = this,
    grunnlag = grunnlag,
    periode = periode
)