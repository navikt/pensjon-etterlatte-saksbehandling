package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import java.time.LocalDate

data class AvdoedesMedlemskapsperiode(
    val periodeType: PeriodeType,
    val arbeidsgiver: String?,
    val stillingsprosent: String?,
    val begrunnelse: String?,
    val kilde: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate
)

enum class PeriodeType {
    ARBEIDSPERIODE,
    ALDERSPENSJON,
    UFØRETRYGD,
    FORELDREPENGER,
    SYKEPENGER,
    DAGPENGER,
    ARBEIDSAVKLARINGSPENGER
}