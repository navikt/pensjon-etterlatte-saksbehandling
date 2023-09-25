package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.time.YearMonth

const val UTBETALINGSDAG = 20

fun erEtterbetaling(
    virkningsdato: LocalDate,
    now: LocalDate = LocalDate.now(),
): Boolean {
    if (YearMonth.from(virkningsdato).isBefore(YearMonth.from(now))) {
        return true
    }
    if (virkningsdato.isBefore(now) && now.dayOfMonth >= UTBETALINGSDAG) {
        return true
    }
    return false
}
