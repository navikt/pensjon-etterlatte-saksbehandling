package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode

fun finnSaksbehandlerMedlemsPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsperiode> =
    grunnlag.saksbehandlerMedlemsPerioder?.opplysning?.perioder
        ?.map { opplysning ->
            VurdertMedlemskapsperiode(
                periodeType = opplysning.periodeType,
                id = opplysning.id,
                arbeidsgiver = opplysning.arbeidsgiver,
                stillingsprosent = opplysning.stillingsprosent,
                begrunnelse = opplysning.begrunnelse,
                kilde = opplysning.kilde,
                oppgittKilde = opplysning.oppgittKilde,
                fraDato = opplysning.fraDato,
                tilDato = opplysning.tilDato,
                godkjentPeriode = opplysning.erGodkjent()
            )
        } ?: emptyList()

fun AvdoedesMedlemskapsperiode.erGodkjent(): Boolean = when (periodeType) {
    PeriodeType.ARBEIDSPERIODE -> this.stillingsprosent?.let { it.toDouble() >= 80 } ?: true
    else -> true
}