package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode

fun finnArbeidsforholdPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsperiode> {
    return grunnlag.arbeidsforholdOpplysning.perioder
        .filter { it.verdi != null }
        .map { arbeidsforhold ->
            VurdertMedlemskapsperiode(
                periodeType = PeriodeType.ARBEIDSPERIODE,
                kilde = arbeidsforhold.kilde,
                arbeidsgiver = arbeidsforhold.verdi!!.arbeidssted.identer.firstOrNull()?.ident,
                stillingsprosent = "${arbeidsforhold.verdi!!.ansettelsesdetaljer.map { it.avtaltStillingsprosent }}",
                fraDato = arbeidsforhold.verdi!!.ansettelsesperiode.startdato,
                tilDato = arbeidsforhold.verdi!!.ansettelsesperiode.sluttdato ?: grunnlag.doedsdato,
                godkjentPeriode = arbeidsforhold.verdi!!.ansettelsesdetaljer.all { it.avtaltStillingsprosent >= 80 }
            )
        }
}