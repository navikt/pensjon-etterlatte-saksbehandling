package no.nav.etterlatte.model

import model.avrund.avrund
import org.slf4j.LoggerFactory
import java.math.BigDecimal

/**
 * @param soeskenFlokk  Antall barn i kullet. Inklusive soeker.
 * @param grunnbeloepPerÅr  Grunnbeløp, årsbeløp.
 * */
private val `0,4` = 0.4.toBigDecimal()
private val `0,25` = 0.25.toBigDecimal()

class Soeskenjustering(private val soeskenFlokk: Int, grunnbeloepPerÅr: Int) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val beloep
        get() = avrund(beregnUtbetaling())

    /** Månedsbeløp for G */
    private val g = BigDecimal(grunnbeloepPerÅr) / BigDecimal.valueOf(12)

    /** 40% av G til første barn, 25% til resten. Fordeles likt */
    private fun beregnUtbetaling(): BigDecimal =
        when (soeskenFlokk) {
            0 -> {
                logger.warn("Søskenjustering ble kalt på uten søskenflokk")
                BigDecimal.ZERO
            }
            1 -> g * `0,4`
            else -> {
                val antallSoesken = (soeskenFlokk - 1).toBigDecimal()
                (g * `0,4` + g * `0,25` * antallSoesken) / (soeskenFlokk.toBigDecimal())
            }
        }
}