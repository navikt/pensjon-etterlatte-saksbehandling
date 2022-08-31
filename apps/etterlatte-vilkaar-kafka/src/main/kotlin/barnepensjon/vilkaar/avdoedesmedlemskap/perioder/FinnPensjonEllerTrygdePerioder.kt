package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters.firstDayOfNextMonth

fun finnPensjonEllerTrygdePerioder(
    grunnlag: AvdoedesMedlemskapGrunnlag,
    type: PeriodeType,
    beskrivelse: String
): List<VurdertMedlemskapsPeriode> {
    val perioder = grunnlag.inntektsOpplysning.opplysning.pensjonEllerTrygd
        .filter { it.beskrivelse == beskrivelse }
        .map {
            val utbetaltIMaaned = YearMonth.parse(it.utbetaltIMaaned)
            val gyldigFra = LocalDate.of(utbetaltIMaaned.year, utbetaltIMaaned.month, 1)

            Periode(gyldigFra, gyldigFra.with(firstDayOfNextMonth()))
        }
        .sortedBy { it.gyldigFra }
        .let { kombinerPerioder(it) } ?: emptyList()

    return perioder.map {
        VurdertMedlemskapsPeriode(
            periodeType = type,
            beskrivelse = null,
            kilde = grunnlag.inntektsOpplysning.kilde,
            fraDato = it.gyldigFra,
            tilDato = it.gyldigTil,
            godkjentPeriode = true
        )
    }
}