package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import java.time.LocalDate

data class GrunnlagMedPeriode<T>(
    val data: T,
    val fom: LocalDate,
    val tom: LocalDate? = null
) {
    init {
        assert(tom == null || fom <= tom) {
            "En periode kan ikke ha en til og med før fra og med!"
        }
    }
}

fun <T, R> List<GrunnlagMedPeriode<T>>.mapVerdier(mapVerdi: (T) -> R): List<GrunnlagMedPeriode<R>> {
    return this.map { GrunnlagMedPeriode(data = mapVerdi(it.data), fom = it.fom, tom = it.tom) }
}

private val kastFeilUtenforPerioder =
    { dato: LocalDate, _: LocalDate?, _: LocalDate? -> throw GrunnlagInneholderIkkePeriodeFeil(dato) }

object PeriodisertBeregningGrunnlag {

    private class Grunnlag<T>(
        opplysninger: List<GrunnlagMedPeriode<T>>,
        private val opplysningUtenforPeriode: (
            datoIPeriode: LocalDate,
            tidligsteOpplysning: LocalDate?,
            senesteOpplysning: LocalDate?
        ) -> T = kastFeilUtenforPerioder
    ) : PeriodisertGrunnlag<T> {
        val sorterteOpplysninger = opplysninger.sortedBy { it.fom }

        val tidligsteFom = sorterteOpplysninger.firstOrNull()?.fom
        val senesteTom = sorterteOpplysninger.mapNotNull { it.tom }.maxOrNull()

        init {
            assert(ingenPerioderOverlapper(opplysninger))
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
        assert(harGrunnlagForHelePerioden(grunnlag.sorterteOpplysninger, fom, tom)) {
            "Grunnlaget er ikke komplett for perioden [$fom, $tom]"
        }
        return grunnlag
    }

    fun <T> lagGrunnlagMedDefaultUtenforPerioder(
        perioder: List<GrunnlagMedPeriode<T>>,
        defaultGrunnlag: (LocalDate, LocalDate?, LocalDate?) -> T
    ): PeriodisertGrunnlag<T> {
        return Grunnlag(
            opplysninger = perioder,
            opplysningUtenforPeriode = defaultGrunnlag
        )
    }

    private fun ingenPerioderOverlapper(sortertePerioder: List<GrunnlagMedPeriode<*>>): Boolean {
        return sortertePerioder.zipWithNext().all { (first, second) ->
            first.tom != null && first.tom < second.fom
        }
    }

    private fun ingenHullInnadIPerioder(sortertePerioder: List<GrunnlagMedPeriode<*>>): Boolean {
        return sortertePerioder.zipWithNext().all { (first, second) ->
            return first.tom != null && first.tom.plusDays(1) == second.fom
        }
    }

    private fun harGrunnlagForHelePerioden(
        sortertePerioder: List<GrunnlagMedPeriode<*>>,
        fom: LocalDate,
        tom: LocalDate?
    ): Boolean {
        val ingenHullInnad = ingenHullInnadIPerioder(sortertePerioder)
        val hoeyesteTom = sortertePerioder.last().tom
        val harGrunnlagIStarten = sortertePerioder.first().fom <= fom
        val varerUtPerioden = hoeyesteTom == null || (tom != null && tom <= hoeyesteTom)

        return ingenHullInnad && harGrunnlagIStarten && varerUtPerioden
    }
}

class GrunnlagInneholderIkkePeriodeFeil(datoIPeriode: LocalDate) :
    Exception("Datoen $datoIPeriode er ikke innenfor grunnlaget")