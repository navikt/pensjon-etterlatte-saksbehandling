package no.nav.etterlatte.behandling.brevoppsett

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class Brevoppsett(
    val behandlingId: UUID,
    val etterbetaling: Etterbetaling?,
    val brevtype: Brevtype,
    val aldersgruppe: Aldersgruppe,
    val kilde: Grunnlagsopplysning.Kilde,
)

class Etterbetaling(
    val fom: YearMonth,
    val tom: YearMonth,
) {
    companion object {
        fun fra(
            datoFom: LocalDate,
            datoTom: LocalDate,
        ): Etterbetaling {
            return Etterbetaling(YearMonth.from(datoFom), YearMonth.from(datoTom))
        }
    }
}

enum class Brevtype {
    NASJONAL,
    UTLAND,
}

enum class Aldersgruppe {
    OVER_18,
    UNDER_18,
}
