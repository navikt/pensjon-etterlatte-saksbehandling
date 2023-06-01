package no.nav.etterlatte.beregning.grunnlag

data class InstitusjonsoppholdBeregnignsGrunnlag(
    val reduksjon: Reduksjon,
    val egenReduksjon: String? = null,
    val begrunnelse: String? = null
)

enum class Reduksjon {
    VELG_REDUKSJON,
    JA_VANLIG,
    NEI_KORT_OPPHOLD,
    JA_EGEN_PROSENT_AV_G,
    NEI_HOEYE_UTGIFTER_BOLIG
}