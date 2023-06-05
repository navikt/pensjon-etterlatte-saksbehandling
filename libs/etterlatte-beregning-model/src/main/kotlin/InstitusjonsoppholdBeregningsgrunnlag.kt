package no.nav.etterlatte.beregning.grunnlag

data class InstitusjonsoppholdBeregningsgrunnlag(
    val reduksjon: Reduksjon,
    val egenReduksjon: Int? = null,
    val begrunnelse: String? = null
) {
    fun redusertProsent() = if (reduksjon.erEgendefinert()) {
        Prosent(egenReduksjon!!)
    } else {
        reduksjon.verdi!!
    }
}

enum class Reduksjon(val verdi: Prosent?) {
    VELG_REDUKSJON(null),
    JA_VANLIG(Prosent(90)),
    NEI_KORT_OPPHOLD(Prosent(0)),
    JA_EGEN_PROSENT_AV_G(null),
    NEI_HOEYE_UTGIFTER_BOLIG(Prosent(0));

    fun erEgendefinert() = this == JA_EGEN_PROSENT_AV_G
}

data class Prosent(val verdi: Int) {
    init {
        require(verdi in 0..100)
    }
}