package no.nav.etterlatte.beregning.model

import java.math.BigDecimal
import java.math.RoundingMode

internal fun avrund(value: BigDecimal): Int {
    return value.setScale(0, RoundingMode.HALF_UP).toInt()
}