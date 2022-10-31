package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.time.YearMonth

data class Virkningstidspunkt(val dato: YearMonth, val kilde: Grunnlagsopplysning.Saksbehandler) {
    companion object {
        fun foersteNesteMaanad(localDate: LocalDate, kilde: Grunnlagsopplysning.Saksbehandler) =
            Virkningstidspunkt(YearMonth.from(localDate).plusMonths(1), kilde)
    }
}