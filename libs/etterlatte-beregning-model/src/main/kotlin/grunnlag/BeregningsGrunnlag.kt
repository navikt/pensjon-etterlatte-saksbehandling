package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsmetodeForAvdoed
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import java.time.YearMonth
import java.util.UUID

data class BeregningsGrunnlag(
    val behandlingId: UUID,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val institusjonsopphold: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>> =
        emptyList(),
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
    val beregningsMetodeFlereAvdoede: List<GrunnlagMedPeriode<BeregningsmetodeForAvdoed>> = emptyList(),
    val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = emptyList(),
    val kunEnJuridiskForelder: GrunnlagMedPeriode<TomVerdi>? = null,
    val vedtaksperioder: List<Vedtaksperiode>? = null,
)

data class Vedtaksperiode(
    val fraOgMed: YearMonth,
    val tilOgMed: YearMonth? = null,
)
