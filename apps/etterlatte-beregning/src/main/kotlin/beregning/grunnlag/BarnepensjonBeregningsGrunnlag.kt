package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning

data class BarnepensjonBeregningsGrunnlag(
    val soeskenMedIBeregningPerioder: List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>>,
    val institusjonsopphold: Institusjonsopphold
)