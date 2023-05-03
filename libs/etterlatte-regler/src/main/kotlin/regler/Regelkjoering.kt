package no.nav.etterlatte.libs.regler

import java.time.LocalDate

data class PeriodisertResultat<S>(
    val periode: RegelPeriode,
    val resultat: SubsumsjonsNode<S>,
    val reglerVersjon: String
)

sealed class RegelkjoeringResultat<S>(open val reglerVersjon: String) {
    data class Suksess<S>(
        val periodiserteResultater: List<PeriodisertResultat<S>>,
        override val reglerVersjon: String
    ) :
        RegelkjoeringResultat<S>(reglerVersjon)

    data class UgyldigPeriode<S>(val ugyldigeReglerForPeriode: List<Regel<*, *>>, override val reglerVersjon: String) :
        RegelkjoeringResultat<S>(reglerVersjon)
}

interface PeriodisertGrunnlag<G> {
    fun finnAlleKnekkpunkter(): Set<LocalDate>
    fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): G
}

class KonstantGrunnlag<G>(private val konstantGrunnlag: G) : PeriodisertGrunnlag<G> {

    override fun finnAlleKnekkpunkter(): Set<LocalDate> = emptySet()
    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): G = konstantGrunnlag
}

object Regelkjoering {
    private val reglerVersjon = Properties.reglerVersjon

    fun <G, S> eksekver(
        regel: Regel<G, S>,
        periodisertGrunnlag: PeriodisertGrunnlag<G>,
        periode: RegelPeriode
    ): RegelkjoeringResultat<S> {
        val ugyldigePerioder = regel.finnUgyldigePerioder(periode)

        return if (ugyldigePerioder.isEmpty()) {
            regel.finnAlleKnekkpunkter()
                .asSequence()
                .filter { knekkpunktGyldigForPeriode(it, periode) }
                .plus(periode.fraDato)
                .plus(periode.tilDato?.plusDays(1) ?: LocalDate.MAX)
                .plus(periodisertGrunnlag.finnAlleKnekkpunkter())
                .sorted()
                .toSet()
                .zipWithNext { periodeFra, nestePeriodeFra ->
                    RegelPeriode(
                        fraDato = periodeFra,
                        tilDato = nestePeriodeFra.takeIf { it.isBefore(LocalDate.MAX) }?.minusDays(1) ?: periode.tilDato
                    )
                }
                .toList()
                .associateWith { p -> regel.anvend(periodisertGrunnlag.finnGrunnlagForPeriode(p.fraDato), p) }
                .let {
                    RegelkjoeringResultat.Suksess(
                        periodiserteResultater = it.entries.map { (key, value) ->
                            PeriodisertResultat(
                                periode = key,
                                resultat = value,
                                reglerVersjon = reglerVersjon
                            )
                        },
                        reglerVersjon = reglerVersjon
                    )
                }
        } else {
            RegelkjoeringResultat.UgyldigPeriode(ugyldigePerioder, reglerVersjon)
        }
    }

    private fun knekkpunktGyldigForPeriode(it: LocalDate, periode: RegelPeriode) =
        it.isAfter(periode.fraDato) && it.isBefore(periode.tilDato ?: LocalDate.MAX)
}

fun <G, S> Regel<G, S>.eksekver(grunnlag: PeriodisertGrunnlag<G>, periode: RegelPeriode) =
    Regelkjoering.eksekver(
        regel = this,
        periodisertGrunnlag = grunnlag,
        periode = periode
    )