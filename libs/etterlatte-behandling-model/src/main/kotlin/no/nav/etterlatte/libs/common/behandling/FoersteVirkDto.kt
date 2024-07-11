package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.time.YearMonth

data class FoersteVirkDto(
    val foersteIverksatteVirkISak: LocalDate,
    val sakId: Long,
)

fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(this.year, this.month)
