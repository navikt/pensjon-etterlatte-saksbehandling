package no.nav.etterlatte.beregning.grunnlag

data class InstitusjonsoppholdBeregningsgrunnlag(
    val reduksjon: Reduksjon,
    val egenReduksjon: Int? = null,
    val begrunnelse: String? = null
)

enum class Reduksjon {
    VELG_REDUKSJON,
    JA_VANLIG,
    NEI_KORT_OPPHOLD,
    JA_EGEN_PROSENT_AV_G,
    NEI_HOEYE_UTGIFTER_BOLIG
}