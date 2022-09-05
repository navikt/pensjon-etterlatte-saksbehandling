package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.vikaar.Metakriterie
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.LocalDate

data class AvdoedesMedlemskapsperiode(
    val periodeType: PeriodeType,
    val id: String,
    val kilde: Grunnlagsopplysning.Kilde,
    val arbeidsgiver: String?,
    val stillingsprosent: String?,
    val begrunnelse: String?,
    val oppgittKilde: String?,
    val fraDato: LocalDate,
    val tilDato: LocalDate
)

enum class PeriodeType {
    ARBEIDSPERIODE,
    ALDERSPENSJON,
    UFOERETRYGD,
    FORELDREPENGER,
    SYKEPENGER,
    DAGPENGER,
    ARBEIDSAVKLARINGSPENGER,
    LOENNSINNTEKT,
    NAERINGSINNTEKT,
    OFFENTLIG_YTELSE
}

data class AvdoedesMedlemskapVurdering(
    val grunnlag: AvdoedesMedlemskapGrunnlag,
    val perioder: List<VurdertMedlemskapsPeriode>,
    val gaps: List<Gap>
)

data class VurdertMedlemskapsPeriode(
    val periodeType: PeriodeType,
    val beskrivelse: String?,
    val kilde: Grunnlagsopplysning.Kilde,
    val fraDato: LocalDate,
    val tilDato: LocalDate?,
    val godkjentPeriode: Boolean
)

data class AvdoedesMedlemskapGrunnlag(
    val inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>,
    val arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>,
    val saksbehandlerMedlemsPerioder: List<VilkaarOpplysning<AvdoedesMedlemskapsperiode>>,
    val doedsdato: LocalDate,
    val bosattNorge: Metakriterie
)

data class Gap(
    val fraDato: LocalDate,
    val tilDato: LocalDate?
)