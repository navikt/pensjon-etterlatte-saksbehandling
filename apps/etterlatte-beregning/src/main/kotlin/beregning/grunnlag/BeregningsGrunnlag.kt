package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import java.util.UUID

data class BeregningsGrunnlag(
    val behandlingId: UUID,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val institusjonsoppholdBeregningsgrunnlag: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>> =
        emptyList(),
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
    val avdoedeBeregningmetode: List<GrunnlagMedPeriode<BeregningsmetodeForAvdoed>> = emptyList(),
    val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = emptyList(),
)

data class BeregningsmetodeForAvdoed(
    val avdoed: String,
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
)
