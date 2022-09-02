package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode

fun finnSaksbehandlerMedlemsPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsPeriode> =
    grunnlag.saksbehandlerMedlemsPerioder
        .map { opplysning ->
            val periode = opplysning.opplysning
            VurdertMedlemskapsPeriode(
                periodeType = periode.periodeType,
                beskrivelse = null,
                kilde = opplysning.kilde,
                fraDato = periode.fraDato,
                tilDato = periode.tilDato,
                godkjentPeriode = periode.erGodkjent()
            )
        }

fun AvdoedesMedlemskapsperiode.erGodkjent(): Boolean = when (periodeType) {
    PeriodeType.ARBEIDSPERIODE -> this.stillingsprosent?.let { it.toDouble() >= 80 } ?: true
    else -> true
}