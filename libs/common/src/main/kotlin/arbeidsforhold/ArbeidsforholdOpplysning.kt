package no.nav.etterlatte.libs.common.arbeidsforhold

data class ArbeidsforholdOpplysning(
    val arbeidsforhold: List<ArbeidsForhold>
)

data class ArbeidsForhold(
    val type: AaregKodeBeskrivelse,
    val arbeidstaker: AaregArbeidstaker,
    val arbeidssted: AaregArbeidssted,
    val ansettelsesdetaljer: List<AaregAnsettelsesdetaljer>,
    val bruksperiode: AaregBruksperiode,
    val ansettelsesperiode: AaregAnsettelsesperiode
)