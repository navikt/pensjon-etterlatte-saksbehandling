package no.nav.etterlatte.model

import no.nav.etterlatte.model.avrund.avrund
import java.math.BigDecimal

private val `0,4` = 0.4.toBigDecimal()
private val `0,25` = 0.25.toBigDecimal()

class Soeskenjustering(private val antallSøsken: Int, grunnbeloepPerÅr: Int) {

    val beloep
        get() = avrund(beregnUtbetaling())

    /** Månedsbeløp for G */
    private val g = BigDecimal(grunnbeloepPerÅr) / BigDecimal.valueOf(12)

    /** 40% av G til første barn, 25% til resten. Fordeles likt */
    private fun beregnUtbetaling(): BigDecimal =
        ((g * `0,4`) + (g * `0,25` * antallSøsken.toBigDecimal())) / (antallSøsken + 1).toBigDecimal()
}