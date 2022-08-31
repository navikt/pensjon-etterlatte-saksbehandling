package barnepensjon.vilkaar.avdoedesmedlemskap.perioder

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.VurdertMedlemskapsPeriode

fun finnArbeidsforholdPerioder(grunnlag: AvdoedesMedlemskapGrunnlag): List<VurdertMedlemskapsPeriode> =
    grunnlag.arbeidsforholdOpplysning.opplysning.arbeidsforhold
        .map { arbeidsforhold ->
            val stilling = "Stillingsprosenter: ${arbeidsforhold.ansettelsesdetaljer.map { it.avtaltStillingsprosent }}"
            VurdertMedlemskapsPeriode(
                periodeType = PeriodeType.ARBEIDSPERIODE,
                beskrivelse = stilling,
                kilde = grunnlag.arbeidsforholdOpplysning.kilde,
                fraDato = arbeidsforhold.ansettelsesperiode.startdato,
                tilDato = arbeidsforhold.ansettelsesperiode.sluttdato,
                godkjentPeriode = arbeidsforhold.ansettelsesdetaljer.all { it.avtaltStillingsprosent >= 80 }
            )
        }