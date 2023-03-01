package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.util.*

data class Omregningshendelse(
    val sakId: Long,
    val fradato: LocalDate,
    val aarsak: RevurderingAarsak,
    val omregningsId: UUID? = null,
    val prosesstype: Prosesstype
)