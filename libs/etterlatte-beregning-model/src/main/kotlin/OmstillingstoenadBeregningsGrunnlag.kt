package no.nav.etterlatte.beregning.grunnlag

data class OmstillingstoenadBeregningsGrunnlag(
    val institusjonsopphold: List<GrunnlagMedPeriode<InstitusjonsoppholdBeregningsgrunnlag>>? =
        emptyList()
)