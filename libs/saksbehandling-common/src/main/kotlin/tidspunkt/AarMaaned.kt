package tidspunkt

import java.time.LocalDate
import java.time.YearMonth

fun YearMonth.erFoerEllerPaa(dato: LocalDate) = this.atDay(1) <= dato

fun YearMonth?.erEtter(dato: LocalDate) = this == null || this.plusMonths(1)?.atDay(1)?.isAfter(dato) == true
