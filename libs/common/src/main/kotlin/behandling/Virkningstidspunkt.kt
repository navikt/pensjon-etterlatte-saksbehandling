package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.time.YearMonth

@JvmInline
value class Virkningstidspunkt(val dato: YearMonth) {
    companion object {
        fun foersteNesteMaanad(localDate: LocalDate) = Virkningstidspunkt(YearMonth.from(localDate).plusMonths(1))
    }
}