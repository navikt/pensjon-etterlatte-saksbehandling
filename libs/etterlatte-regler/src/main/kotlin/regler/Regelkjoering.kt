package no.nav.etterlatte.libs.regler

import java.time.LocalDate


sealed class SubsumsjonEllerIngenRegel<S> {

    data class Subsumsjon<S>(
        val subsumsjon: SubsumsjonsNode<S>,
    ) : SubsumsjonEllerIngenRegel<S>()

    data object IngenRegel : SubsumsjonEllerIngenRegel<Any>()
}


interface ResultatPeriode<T> {
    val periode: RegelPeriode
}

data class IngenResultat<S>(
    override val periode: RegelPeriode,
) : ResultatPeriode<S>

data class PeriodisertResultat<S>(
    override val periode: RegelPeriode,
    val resultat: SubsumsjonsNode<S>,
    val reglerVersjon: String,
) : ResultatPeriode<S>

sealed class RegelkjoeringResultat<S>(
    open val reglerVersjon: String,
) {
    data class Suksess<S>(
        val periodiserteResultater: List<ResultatPeriode<S>>,
        override val reglerVersjon: String,
    ) : RegelkjoeringResultat<S>(reglerVersjon)

    data class UgyldigPeriode<S>(
        val ugyldigeReglerForPeriode: List<Regel<*, *>>,
        override val reglerVersjon: String,
    ) : RegelkjoeringResultat<S>(reglerVersjon)
}

private fun List<RegelPeriode>.finnAlleKnekkpunktForPerioder() = this.flatMap { listOfNotNull(it.fraDato, it.tilDato) }

private fun List<RegelPeriode>.inkluderer(p: RegelPeriode): Boolean =
    this.any { p.fraDato >= it.fraDato && (p.tilDato == null || p.tilDato >= (it.tilDato ?: p.tilDato)) }

private fun RegelPeriode.erInnenforRegelperiode(dato: LocalDate) =
    dato >= this.fraDato && (dato <= this.tilDato ?: dato)



[1, 2, 3, 4, 5]
[(1,2), (2,3), (3,4), (4, 5)]

object Regelkjoering {
    private val reglerVersjon = Properties.reglerVersjon

    fun <G, S> eksekver(
        regel: Regel<G, S>,
        grunnlag: PeriodisertGrunnlag<G>,
        periode: RegelPeriode,
    ): RegelkjoeringResultat<S> = eksekver(regel, grunnlag, listOf(periode))

    fun <G, S> eksekver(
        regel: Regel<G, S>,
        grunnlag: PeriodisertGrunnlag<G>,
        perioder: List<RegelPeriode>,
    ): RegelkjoeringResultat<S> {
        val ugyldigePerioder = perioder.flatMap { regel.finnUgyldigePerioder(it) }

        return if (ugyldigePerioder.isEmpty()) {
            regel
                .finnAlleKnekkpunkterIRegel()
                .asSequence()
                .plus(perioder.flatMap { grunnlag.finnKnekkpunkterInnenforPeriode(it) })
                .plus(perioder.finnAlleKnekkpunktForPerioder())
                .filter { periode -> perioder.any { knekkpunktGyldigForPeriode(periode, it) } }
                .sorted()
                .toSet()
                .zipWithNext { periodeFra, nestePeriodeFra ->
                    RegelPeriode(
                        fraDato = periodeFra,
                        tilDato = nestePeriodeFra.takeIf { it.isBefore(LocalDate.MAX) }?.minusDays(1)
                            ?: perioder.last().tilDato,
                    )
                }.toList()
                .associateWith { p ->
                    if (perioder.inkluderer(p)) {
                        SubsumsjonEllerIngenRegel.Subsumsjon(
                            regel.anvend(
                                grunnlag.finnGrunnlagForPeriode(p.fraDato),
                                p
                            )
                        )
                    } else {
                        SubsumsjonEllerIngenRegel.IngenRegel
                    }
                }.let {
                    RegelkjoeringResultat.Suksess(
                        periodiserteResultater =
                            it.entries.map { (key, value) ->
                                if (value is SubsumsjonEllerIngenRegel.Subsumsjon) {
                                    PeriodisertResultat(
                                        periode = key,
                                        resultat = value.subsumsjon.verdi,
                                        reglerVersjon = reglerVersjon,
                                    )
                                } else {
                                    IngenResultat(
                                        periode = key
                                    )
                                }
                            },
                        reglerVersjon = reglerVersjon,
                    )
                }
        }
    } else {
        RegelkjoeringResultat.UgyldigPeriode(ugyldigePerioder, reglerVersjon)
    }
}

private fun knekkpunktGyldigForPeriode(
    it: LocalDate,
    periode: RegelPeriode,
) = it >= periode.fraDato && it <= (periode.tilDato ?: LocalDate.MAX)
}

fun <G, S> Regel<G, S>.eksekver(
    grunnlag: PeriodisertGrunnlag<G>,
    periode: RegelPeriode,
) = Regelkjoering.eksekver(
    regel = this,
    grunnlag = grunnlag,
    periode = periode,
)
