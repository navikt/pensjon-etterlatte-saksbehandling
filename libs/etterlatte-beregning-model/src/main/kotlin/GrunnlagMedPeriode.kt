package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import java.time.LocalDate

data class GrunnlagMedPeriode<T>(
    val data: T,
    val fom: LocalDate,
    val tom: LocalDate? = null,
) {
    init {
        if (tom != null && fom > tom) {
            throw UgyldigPeriodeForGrunnlag(fom, tom)
        }
    }

    fun erInnenforPeriode(
        periodeFom: LocalDate,
        periodeTom: LocalDate?,
    ): Boolean {
        if (periodeTom == null) {
            return tom == null || tom >= periodeFom
        }
        assert(periodeFom <= periodeTom) {
            "Perioder må ha fom >= tom"
        }
        if (tom == null) {
            return fom <= periodeTom
        }
        return tom >= periodeFom && fom <= periodeTom
    }
}

fun <T> List<GrunnlagMedPeriode<T>>.kombinerOverlappendePerioder(): List<GrunnlagMedPeriode<List<T>>> {
    val harAapenSluttperiode = this.any { it.tom == null }
    var knekkpunkter: List<LocalDate?> = this.flatMap { listOfNotNull(it.fom, it.tom?.plusDays(1)) }.distinct().sorted()
    if (harAapenSluttperiode) {
        knekkpunkter = knekkpunkter.plus(listOf(null))
    }
    val perioder = knekkpunkter.zipWithNext()
    return perioder.map { (fom, tilOgIkkeMed) ->
        val tom = tilOgIkkeMed?.minusDays(1)

        val innenforPeriode =
            this
                .filter { it.erInnenforPeriode(fom!!, tom) }
                .map { it.data }
        GrunnlagMedPeriode(
            data = innenforPeriode,
            fom = fom!!,
            tom = tom,
        )
    }
}

class UgyldigPeriodeForGrunnlag(
    fom: LocalDate,
    tom: LocalDate?,
) : Exception("En periode kan ikke ha fra og med ($fom) etter til og med ($tom)!")

fun <T, R> List<GrunnlagMedPeriode<T>>.mapVerdier(mapVerdi: (T) -> R): List<GrunnlagMedPeriode<R>> =
    this.map {
        GrunnlagMedPeriode(data = mapVerdi(it.data), fom = it.fom, tom = it.tom)
    }

private val kastFeilUtenforPerioder =
    { dato: LocalDate, _: LocalDate, _: LocalDate? -> throw PeriodiseringAvGrunnlagFeil.DatoUtenforPerioder(dato) }

object PeriodisertBeregningGrunnlag {
    private class Grunnlag<T>(
        opplysninger: List<GrunnlagMedPeriode<T>>,
        private val opplysningUtenforPeriode: (
            datoIPeriode: LocalDate,
            tidligsteOpplysning: LocalDate,
            senesteOpplysning: LocalDate?,
        ) -> T = kastFeilUtenforPerioder,
    ) : PeriodisertGrunnlag<T> {
        val sorterteOpplysninger = opplysninger.sortedBy { it.fom }

        val tidligsteFom: LocalDate
        val senesteTom: LocalDate?

        init {
            if (sorterteOpplysninger.isEmpty()) {
                throw PeriodiseringAvGrunnlagFeil.IngenPerioder()
            }
            if (perioderOverlapper(sorterteOpplysninger)) {
                throw PeriodiseringAvGrunnlagFeil.PerioderOverlapper()
            }

            tidligsteFom = sorterteOpplysninger.first().fom
            senesteTom = sorterteOpplysninger.last().tom
        }

        override fun finnAlleKnekkpunkter(): Set<LocalDate> =
            (
                sorterteOpplysninger.map { it.fom } +
                    sorterteOpplysninger.mapNotNull { it.tom?.plusDays(1) }
            ).toSet()

        override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): T =
            sorterteOpplysninger
                .find {
                    it.fom <= datoIPeriode && (it.tom == null || it.tom >= datoIPeriode)
                }?.data ?: opplysningUtenforPeriode(datoIPeriode, tidligsteFom, senesteTom)
    }

    fun <T> lagKomplettPeriodisertGrunnlag(
        perioder: List<GrunnlagMedPeriode<T>>,
        fom: LocalDate,
        tom: LocalDate?,
    ): PeriodisertGrunnlag<T> {
        val grunnlag = Grunnlag(opplysninger = perioder)
        val harGrunnlagForHelePerioden = harGrunnlagForHelePerioden(grunnlag.sorterteOpplysninger, fom, tom)
        if (!harGrunnlagForHelePerioden.harGrunnlagForHelePerioden()) {
            throw PeriodiseringAvGrunnlagFeil.PerioderErIkkeKomplett(harGrunnlagForHelePerioden)
        }
        return grunnlag
    }

    fun <T> lagPotensieltTomtGrunnlagMedDefaultUtenforPerioder(
        perioder: List<GrunnlagMedPeriode<T>>,
        defaultGrunnlag: (datoIPeriode: LocalDate, foersteFom: LocalDate, senesteTom: LocalDate?) -> T,
    ): PeriodisertGrunnlag<T> =
        if (perioder.isEmpty()) {
            KonstantGrunnlag(defaultGrunnlag.invoke(LocalDate.now(), LocalDate.now(), null))
        } else {
            lagGrunnlagMedDefaultUtenforPerioder(perioder, defaultGrunnlag)
        }

    fun <T> lagGrunnlagMedDefaultUtenforPerioder(
        perioder: List<GrunnlagMedPeriode<T>>,
        defaultGrunnlag: (datoIPeriode: LocalDate, foersteFom: LocalDate, senesteTom: LocalDate?) -> T,
    ): PeriodisertGrunnlag<T> =
        Grunnlag(
            opplysninger = perioder,
            opplysningUtenforPeriode = defaultGrunnlag,
        )

    fun perioderOverlapper(sortertePerioder: List<GrunnlagMedPeriode<*>>): Boolean =
        !sortertePerioder.zipWithNext().all { (first, second) ->
            first.tom != null && first.tom < second.fom
        }

    private fun ingenHullInnadIPerioder(sortertePerioder: List<GrunnlagMedPeriode<*>>): Boolean {
        return sortertePerioder.zipWithNext().all { (first, second) ->
            return first.tom != null && first.tom.plusDays(1) == second.fom
        }
    }

    private fun harGrunnlagForHelePerioden(
        sortertePerioder: List<GrunnlagMedPeriode<*>>,
        fom: LocalDate,
        tom: LocalDate?,
    ): GrunnlagForHelePerioden {
        val perioder =
            listOf(sortertePerioder.takeWhile { it.fom <= fom }.last()) + sortertePerioder.dropWhile { it.fom <= fom }
        val ingenHullInnad = ingenHullInnadIPerioder(perioder)
        val hoeyesteTom = perioder.last().tom
        val harGrunnlagIStarten = perioder.first().fom <= fom

        return GrunnlagForHelePerioden(ingenHullInnad, harGrunnlagIStarten, VarerUtPerioden(tom, hoeyesteTom))
    }
}

data class GrunnlagForHelePerioden(
    val ingenHullInnad: Boolean,
    val harGrunnlagIStarten: Boolean,
    val varerUtPerioden: VarerUtPerioden,
) {
    fun harGrunnlagForHelePerioden() = ingenHullInnad && harGrunnlagIStarten && varerUtPerioden.varerUtPerioden()
}

data class VarerUtPerioden(
    val tom: LocalDate?,
    val hoeyesteTom: LocalDate?,
) {
    fun varerUtPerioden() = hoeyesteTom == null || (tom != null && tom <= hoeyesteTom)
}

sealed class PeriodiseringAvGrunnlagFeil(
    detail: String,
) : UgyldigForespoerselException("PERIODISERING_AV_GRUNNLAG_FEIL", detail) {
    class DatoUtenforPerioder(
        datoIPeriode: LocalDate,
    ) : PeriodiseringAvGrunnlagFeil("Datoen $datoIPeriode er ikke innenfor grunnlaget")

    class IngenPerioder : PeriodiseringAvGrunnlagFeil("Ingen perioder for grunnlaget ble gitt for periodisering")

    class PerioderOverlapper : PeriodiseringAvGrunnlagFeil("Periodene for periodisering overlapper")

    class PerioderErIkkeKomplett(
        grunnlagForHelePerioden: GrunnlagForHelePerioden,
    ) : PeriodiseringAvGrunnlagFeil(
            "Periodene gitt er ikke komplette for den overordnede perioden: $grunnlagForHelePerioden",
        )
}

fun <T> erGrunnlagLiktFoerEnDato(
    grunnlag1: List<GrunnlagMedPeriode<T>>,
    grunnlag2: List<GrunnlagMedPeriode<T>>,
    cutoff: LocalDate,
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
