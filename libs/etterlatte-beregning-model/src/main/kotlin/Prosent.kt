package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.libs.common.feilhaandtering.krev

data class Prosent(
    val verdi: Int,
) {
    init {
        krev(verdi in 0..100) { "Ugyldig prosent verdi: $verdi" }
    }

    fun minus(verdi: Prosent?) = minus(verdi?.verdi ?: 0)

    fun minus(verdi: Int) = Prosent(this.verdi - verdi)

    companion object {
        val hundre = Prosent(100)
    }
}
