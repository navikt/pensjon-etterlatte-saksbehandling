package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.util.*

data class Omberegningshendelse(
    val sakId: Long,
    val sakType: SakType,
    val fradato: LocalDate,
    val aarsak: RevurderingAarsak,
    val omberegningsId: UUID? = null
)