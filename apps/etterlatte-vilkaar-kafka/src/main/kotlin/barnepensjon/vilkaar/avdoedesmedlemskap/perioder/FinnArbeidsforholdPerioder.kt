package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsperiode

fun finnArbeidsforholdPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsperiode> {
    return grunnlag.arbeidsforholdOpplysning.perioder
        .mapNotNull { opplysning ->
            opplysning.verdi?.let { aaregResponse ->
                VurdertMedlemskapsperiode(
                    periodeType = PeriodeType.ARBEIDSPERIODE,
                    kilde = opplysning.kilde,
                    arbeidsgiver = aaregResponse.arbeidssted.identer.firstOrNull()?.ident,
                    stillingsprosent = "${aaregResponse.ansettelsesdetaljer.map { it.avtaltStillingsprosent }}",
                    fraDato = aaregResponse.ansettelsesperiode.startdato,
                    tilDato = aaregResponse.ansettelsesperiode.sluttdato ?: grunnlag.doedsdato,
                    godkjentPeriode = aaregResponse.ansettelsesdetaljer.all { it.avtaltStillingsprosent >= 80 }
                )
            }
        }
}