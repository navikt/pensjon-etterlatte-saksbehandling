import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode
import no.nav.etterlatte.libs.common.inntekt.Inntekt
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

fun List<Periode>.tilVurderteMedlemskapsPerioder(
    type: PeriodeType,
    kilde: Grunnlagsopplysning.Kilde,
    beskrivelse: String? = null
) = this.map {
    VurdertMedlemskapsPeriode(
        periodeType = type,
        beskrivelse = beskrivelse,
        kilde = kilde,
        fraDato = it.gyldigFra,
        tilDato = it.gyldigTil,
        godkjentPeriode = true
    )
}

fun List<Inntekt>.kombinerPerioder() = this.filter { it.fordel == "kontantytelse" && it.beloep > 0 }
    .map {
        val utbetaltIMaaned = YearMonth.parse(it.utbetaltIMaaned)
        val gyldigFra = LocalDate.of(utbetaltIMaaned.year, utbetaltIMaaned.month, 1)

        Periode(gyldigFra, gyldigFra.with(TemporalAdjusters.lastDayOfMonth()))
    }.sortedBy { it.gyldigFra }.let { no.nav.etterlatte.barnepensjon.kombinerPerioder(it) } ?: emptyList()