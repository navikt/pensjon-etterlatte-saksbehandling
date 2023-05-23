package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import java.time.LocalDate

data class GrunnlagMedPeriode<T>(
    val data: T,
    val fom: LocalDate,
    val tom: LocalDate? = null
) {
    init {
        if (tom != null && fom > tom) {
            throw UgyldigPeriodeForGrunnlag(fom, tom)
        }
    }
}

class UgyldigPeriodeForGrunnlag(fom: LocalDate, tom: LocalDate?) :
    Exception("En periode kan ikke ha fra og med ($fom) etter til og med ($tom)!")

fun <T, R> List<GrunnlagMedPeriode<T>>.mapVerdier(mapVerdi: (T) -> R): List<GrunnlagMedPeriode<R>> {
    return this.map { GrunnlagMedPeriode(data = mapVerdi(it.data), fom = it.fom, tom = it.tom) }
}

private val kastFeilUtenforPerioder =
    { dato: LocalDate, _: LocalDate, _: LocalDate? -> throw PeriodiseringAvGrunnlagFeil.DatoUtenforPerioder(dato) }

object PeriodisertBeregningGrunnlag {

    private class Grunnlag<T>(
        opplysninger: List<GrunnlagMedPeriode<T>>,
        private val opplysningUtenforPeriode: (
            datoIPeriode: LocalDate,
            tidligsteOpplysning: LocalDate,
            senesteOpplysning: LocalDate?
        ) -> T = kastFeilUtenforPerioder
    ) : PeriodisertGrunnlag<T> {
        val sorterteOpplysninger = opplysninger.sortedBy { it.fom }

        val tidligsteFom: LocalDate
        val senesteTom: LocalDate?

        init {
            if (opplysninger.isEmpty()) {
                throw PeriodiseringAvGrunnlagFeil.IngenPerioder()
            }
            if (perioderOverlapper(opplysninger)) {
                throw PeriodiseringAvGrunnlagFeil.PerioderOverlapper()
            }

            tidligsteFom = sorterteOpplysninger.first().fom
            senesteTom = sorterteOpplysninger.last().tom
        }

        override fun finnAlleKnekkpunkter(): Set<LocalDate> {
            return (
                sorterteOpplysninger.map { it.fom } +
                    sorterteOpplysninger.mapNotNull { it.tom?.plusDays(1) }
                ).toSet()
        }

        override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): T {
            return sorterteOpplysninger.find {
                it.fom <= datoIPeriode && (it.tom == null || it.tom >= datoIPeriode)
            }?.data ?: opplysningUtenforPeriode(datoIPeriode, tidligsteFom, senesteTom)
        }
    }

    fun <T> lagKomplettPeriodisertGrunnlag(
        perioder: List<GrunnlagMedPeriode<T>>,
        fom: LocalDate,
        tom: LocalDate?
    ): PeriodisertGrunnlag<T> {
        val grunnlag = Grunnlag(opplysninger = perioder)
        if (!harGrunnlagForHelePerioden(grunnlag.sorterteOpplysninger, fom, tom)) {
            throw PeriodiseringAvGrunnlagFeil.PerioderErIkkeKomplett()
        }
        return grunnlag
    }

    fun <T> lagGrunnlagMedDefaultUtenforPerioder(
        perioder: List<GrunnlagMedPeriode<T>>,
        defaultGrunnlag: (datoIPeriode: LocalDate, foersteFom: LocalDate, senesteTom: LocalDate?) -> T
    ): PeriodisertGrunnlag<T> {
        return Grunnlag(
            opplysninger = perioder,
            opplysningUtenforPeriode = defaultGrunnlag
        )
    }

    fun perioderOverlapper(sortertePerioder: List<GrunnlagMedPeriode<*>>): Boolean {
        return !sortertePerioder.zipWithNext().all { (first, second) ->
            first.tom != null && first.tom < second.fom
        }
    }

    fun ingenHullInnadIPerioder(sortertePerioder: List<GrunnlagMedPeriode<*>>): Boolean {
        return sortertePerioder.zipWithNext().all { (first, second) ->
            return first.tom != null && first.tom.plusDays(1) == second.fom
        }
    }

    fun harGrunnlagForHelePerioden(
        sortertePerioder: List<GrunnlagMedPeriode<*>>,
        fom: LocalDate,
        tom: LocalDate?
    ): Boolean {
        val perioder =
            listOf(sortertePerioder.takeWhile { it.fom <= fom }.last()) + sortertePerioder.dropWhile { it.fom <= fom }
        val ingenHullInnad = ingenHullInnadIPerioder(perioder)
        val hoeyesteTom = perioder.last().tom
        val harGrunnlagIStarten = perioder.first().fom <= fom
        val varerUtPerioden = hoeyesteTom == null || (tom != null && tom <= hoeyesteTom)

        return ingenHullInnad && harGrunnlagIStarten && varerUtPerioden
    }
}

sealed class PeriodiseringAvGrunnlagFeil(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DatoUtenforPerioder(datoIPeriode: LocalDate) :
        PeriodiseringAvGrunnlagFeil("Datoen $datoIPeriode er ikke innenfor grunnlaget")

    class IngenPerioder : PeriodiseringAvGrunnlagFeil("Ingen perioder for grunnlaget ble gitt for periodisering")
    class PerioderOverlapper : PeriodiseringAvGrunnlagFeil("Periodene for periodisering overlapper")
    class PerioderErIkkeKomplett :
        PeriodiseringAvGrunnlagFeil("Periodene gitt er ikke komplette for den overordnede perioden")
}

fun <T> erGrunnlagLiktFoerEnDato(
    grunnlag1: List<GrunnlagMedPeriode<T>>,
    grunnlag2: List<GrunnlagMedPeriode<T>>,
    cutoff: LocalDate
): Boolean {
    // hjelpemetode som kutter av tom på dagen før cutoff hvis den går over
    fun GrunnlagMedPeriode<T>.medNormalisertTom(): GrunnlagMedPeriode<T> {
        if (tom == null || tom >= cutoff) {
            return this.copy(tom = cutoff.minusDays(1))
        }
        return this.copy()
    }

    // ta kun med de periodene som er før cutoff
    val relevantePerioder1 = grunnlag1.filter { it.fom < cutoff }.sortedBy { it.fom }
    val relevantePerioder2 = grunnlag2.filter { it.fom < cutoff }.sortedBy { it.fom }

    // Hvis en av grunnlagene er tomme må begge være tomme for at grunnlagene skal være like
    if (relevantePerioder1.isEmpty() || relevantePerioder2.isEmpty()) {
        return relevantePerioder1.isEmpty() && relevantePerioder2.isEmpty()
    }

    // Fiks siste tom for begge grunnlagene
    val g1 = relevantePerioder1.dropLast(1) + relevantePerioder1.last().medNormalisertTom()
    val g2 = relevantePerioder2.dropLast(1) + relevantePerioder2.last().medNormalisertTom()

    // nå skal de være like
    return g1 == g2
}