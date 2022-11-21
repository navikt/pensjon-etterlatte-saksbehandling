package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import java.time.LocalDate
import java.util.*

interface AvdoedesMedlemskapsperiode {
    val id: String
    val periodeType: PeriodeType
    val kilde: Grunnlagsopplysning.Kilde
    val arbeidsgiver: String?
    val stillingsprosent: String?
    val begrunnelse: String?
    val oppgittKilde: String?
    val fraDato: LocalDate
    val tilDato: LocalDate
}

data class SaksbehandlerMedlemskapsperiode(
    override val id: String,
    override val periodeType: PeriodeType,
    override val kilde: Grunnlagsopplysning.Kilde,
    override val arbeidsgiver: String? = null,
    override val stillingsprosent: String? = null,
    override val begrunnelse: String? = null,
    override val oppgittKilde: String,
    override val fraDato: LocalDate,
    override val tilDato: LocalDate
) : AvdoedesMedlemskapsperiode

data class SaksbehandlerMedlemskapsperioder(
    val perioder: List<SaksbehandlerMedlemskapsperiode>
)

data class VurdertMedlemskapsperiode(
    override val id: String = UUID.randomUUID().toString(),
    override val periodeType: PeriodeType,
    override val kilde: Grunnlagsopplysning.Kilde,
    override val arbeidsgiver: String? = null,
    override val stillingsprosent: String? = null,
    override val begrunnelse: String? = null,
    override val oppgittKilde: String? = null,
    override val fraDato: LocalDate,
    override val tilDato: LocalDate,

    val beskrivelse: String? = null,
    val godkjentPeriode: Boolean
) : AvdoedesMedlemskapsperiode

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

data class AvdoedesMedlemskapGrunnlag(
    // val inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>,
    val arbeidsforholdOpplysning: Opplysning.Periodisert<AaregResponse?>,
    val doedsdato: LocalDate
)

data class Gap(
    val fraDato: LocalDate,
    val tilDato: LocalDate?
)