package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.vikaar.Metakriterie
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.LocalDate

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
    val saksbehandlerMedlemsPerioder: List<VilkaarOpplysning<SaksbehandlerMedlemskapsperiode>>,
    val doedsdato: LocalDate,
    val bosattNorge: Metakriterie
)

data class Gap(
    val fraDato: LocalDate,
    val tilDato: LocalDate?
)