package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode

fun finnSaksbehandlerMedlemsPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsperiode> =
    grunnlag.saksbehandlerMedlemsPerioder
        .map { opplysning ->
            val periode = opplysning.opplysning
            VurdertMedlemskapsperiode(
                periodeType = periode.periodeType,
                id = periode.id,
                arbeidsgiver = periode.arbeidsgiver,
                stillingsprosent = periode.stillingsprosent,
                begrunnelse = periode.begrunnelse,
                kilde = periode.kilde,
                oppgittKilde = periode.oppgittKilde,
                fraDato = periode.fraDato,
                tilDato = periode.tilDato,
                godkjentPeriode = periode.erGodkjent()
            )
        }

fun AvdoedesMedlemskapsperiode.erGodkjent(): Boolean = when (periodeType) {
    PeriodeType.ARBEIDSPERIODE -> this.stillingsprosent?.let { it.toDouble() >= 80 } ?: true
    else -> true
}