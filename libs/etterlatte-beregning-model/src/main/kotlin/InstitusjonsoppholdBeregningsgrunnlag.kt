package no.nav.etterlatte.beregning.grunnlag

data class InstitusjonsoppholdBeregningsgrunnlag(
    val reduksjon: Reduksjon,
    val egenReduksjon: Int? = null,
    val begrunnelse: String? = null
) {
    fun prosentEtterReduksjon() = if (reduksjon.erEgendefinert()) {
        Prosent.hundre.minus(egenReduksjon!!)
    } else {
        reduksjon.gjenvaerendeEtterReduksjon()
    }
}

enum class Reduksjon(val verdi: Prosent?) {
    VELG_REDUKSJON(null),
    JA_VANLIG(Prosent(90)),
    NEI_KORT_OPPHOLD(Prosent(0)),
    JA_EGEN_PROSENT_AV_G(null),
    NEI_HOEYE_UTGIFTER_BOLIG(Prosent(0));

    fun erEgendefinert() = this == JA_EGEN_PROSENT_AV_G

    fun gjenvaerendeEtterReduksjon() = Prosent.hundre.minus(verdi)
}

data class Prosent(val verdi: Int) {
    init {
        require(verdi in 0..100)
    }

    fun minus(verdi: Prosent?) = minus(verdi?.verdi ?: 0)

    fun minus(verdi: Int) = Prosent(this.verdi - verdi)

    companion object {
        val hundre = Prosent(100)
    }
}