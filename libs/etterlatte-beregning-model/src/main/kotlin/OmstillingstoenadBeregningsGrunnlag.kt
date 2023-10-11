package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag

data class OmstillingstoenadBeregningsGrunnlag(
    val institusjonsopphold: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>>? =
        emptyList(),
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag = BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.NASJONAL),
)
