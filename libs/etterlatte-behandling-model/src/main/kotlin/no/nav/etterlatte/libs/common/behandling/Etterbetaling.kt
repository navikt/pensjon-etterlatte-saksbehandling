package no.nav.etterlatte.libs.common.behandling

import java.time.YearMonth
import java.util.UUID

data class Etterbetaling(
    val behandlingId: UUID,
    val fra: YearMonth,
    val til: YearMonth,
)
