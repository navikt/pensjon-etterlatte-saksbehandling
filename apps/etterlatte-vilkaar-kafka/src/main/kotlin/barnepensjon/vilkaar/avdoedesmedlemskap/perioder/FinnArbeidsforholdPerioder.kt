package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode

fun finnArbeidsforholdPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsperiode> =
    grunnlag.arbeidsforholdOpplysning.opplysning.arbeidsforhold
        .map { arbeidsforhold ->
            VurdertMedlemskapsperiode(
                periodeType = PeriodeType.ARBEIDSPERIODE,
                kilde = grunnlag.arbeidsforholdOpplysning.kilde,
                arbeidsgiver = arbeidsforhold.arbeidssted.identer.firstOrNull()?.ident,
                stillingsprosent = "${arbeidsforhold.ansettelsesdetaljer.map { it.avtaltStillingsprosent }}",
                fraDato = arbeidsforhold.ansettelsesperiode.startdato,
                tilDato = arbeidsforhold.ansettelsesperiode.sluttdato ?: grunnlag.doedsdato,
                godkjentPeriode = arbeidsforhold.ansettelsesdetaljer.all { it.avtaltStillingsprosent >= 80 }
            )
        }