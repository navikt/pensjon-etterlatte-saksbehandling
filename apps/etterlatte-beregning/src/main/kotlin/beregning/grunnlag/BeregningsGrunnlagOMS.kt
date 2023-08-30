package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class BeregningsGrunnlagOMS(
    val behandlingId: UUID,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val institusjonsoppholdBeregningsgrunnlag: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>>? =
        emptyList()
)