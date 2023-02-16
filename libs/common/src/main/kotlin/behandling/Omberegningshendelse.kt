package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.util.*

data class Omberegningshendelse(
    val sakId: Long,
    val fradato: LocalDate,
    val aarsak: RevurderingAarsak,
    val omberegningsId: UUID? = null
)

enum class Hendelsestype {
    OMBEREGNINGSHENDELSE;

    override fun toString() = name
}

object Omberegningsnoekler {
    const val hendelse_data = "hendelse_data"
    const val omberegningId = "omberegningId"
    const val beregning = "beregning"
}