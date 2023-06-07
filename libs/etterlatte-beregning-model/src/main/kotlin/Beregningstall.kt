package no.nav.etterlatte.regler

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.beregning.grunnlag.Prosent
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Wrapper-klasse over BigDecimal som brukes for å oppnå følgende:
 * - Likt antall desimaler i delberegninger
 * - Samme strategi for avrundinger i delberegninger
 * - Korrekt opprettelse av BigDecimal ved konvertering fra Double (for å unngå og miste presisjon)
 * - Overloadede metoder for å gjøre det enklere og utføre matematiske operasjoner for ulike typer uten konvertering
 */
class Beregningstall : Comparable<Beregningstall> {

    @JsonProperty
    private val value: BigDecimal

    constructor(number: Int) {
        value = number.toBigDecimal()
    }

    constructor(number: Double) {
        value = number.toBigDecimal()
    }

    constructor(number: BigDecimal) {
        value = number
    }

    fun multiply(
        multiplicand: Beregningstall,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) = Beregningstall(
        value.multiply(multiplicand.value).setScale(decimals, roundingMode)
    )

    fun multiply(
        multiplicand: Int,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) = Beregningstall(
        value.multiply(BigDecimal(multiplicand)).setScale(decimals, roundingMode)
    )

    fun divide(
        divisor: Beregningstall,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) =
        Beregningstall(value.divide(divisor.value, decimals, roundingMode))

    fun divide(
        divisor: Int,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) =
        Beregningstall(value.divide(BigDecimal(divisor), decimals, roundingMode))

    fun plus(
        augend: Beregningstall,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) =
        Beregningstall(value.plus(augend.value).setScale(decimals, roundingMode))

    fun plus(
        augend: Int,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) =
        Beregningstall(value.plus(BigDecimal(augend)).setScale(decimals, roundingMode))

    fun minus(
        subtrahend: Beregningstall,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) =
        Beregningstall(value.subtract(subtrahend.value).setScale(decimals, roundingMode))

    fun minus(
        subtrahend: Int,
        decimals: Int = DESIMALER_DELBEREGNING,
        roundingMode: RoundingMode = AVRUNDING_DELBEREGNING
    ) = Beregningstall(
        value.subtract(BigDecimal(subtrahend)).setScale(decimals, roundingMode)
    )

    fun zeroIfNegative(): Beregningstall {
        return if (this.value < BigDecimal.ZERO) {
            Beregningstall(BigDecimal.ZERO)
        } else {
            this
        }
    }

    fun setScale(decimals: Int, roundingMode: RoundingMode = AVRUNDING_BEREGNING) =
        Beregningstall(value.setScale(decimals, roundingMode))

    fun round(decimals: Int, roundingMode: RoundingMode = AVRUNDING_BEREGNING): Beregningstall =
        Beregningstall(value.setScale(decimals, roundingMode))

    fun toInteger() = value.toInt()

    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(other: Beregningstall): Int {
        return this.value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Beregningstall -> this.value == other.value
            else -> false
        }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    companion object {
        fun somBroek(verdi: Prosent) = Beregningstall(verdi.verdi).divide(100)

        const val DESIMALER_DELBEREGNING = 16
        val AVRUNDING_DELBEREGNING = RoundingMode.FLOOR
        val AVRUNDING_BEREGNING = RoundingMode.HALF_UP
    }
}