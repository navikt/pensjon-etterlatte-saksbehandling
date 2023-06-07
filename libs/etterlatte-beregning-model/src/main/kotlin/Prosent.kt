package no.nav.etterlatte.beregning.grunnlag

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