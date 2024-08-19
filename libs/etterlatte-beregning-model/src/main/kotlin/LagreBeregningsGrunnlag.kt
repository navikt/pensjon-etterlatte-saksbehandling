package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsmetodeForAvdoed
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning

data class LagreBeregningsGrunnlag(
    val soeskenMedIBeregning: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> = emptyList(),
    val institusjonsopphold: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>>? =
        emptyList(),
    val beregningsMetode: BeregningsMetodeBeregningsgrunnlag = BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.NASJONAL),
    val beregningsMetodeFlereAvdoede: List<GrunnlagMedPeriode<BeregningsmetodeForAvdoed>>? = emptyList(),
)
