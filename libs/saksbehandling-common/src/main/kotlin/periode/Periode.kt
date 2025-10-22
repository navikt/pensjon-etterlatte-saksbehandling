package no.nav.etterlatte.libs.common.periode

import java.time.YearMonth

data class Periode(
    val fom: YearMonth,
    val tom: YearMonth?,
) {
    fun erMaanedIPerioden(maaned: YearMonth) = maaned >= fom && maaned <= (tom ?: maaned)
}
