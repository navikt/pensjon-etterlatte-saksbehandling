package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate

data class Omberegningshendelse(
    val sakId: Long,
    val fradato: LocalDate,
    val aarsak: RevurderingAarsak
)

enum class Hendelsestype {
    OMBEREGNINGSHENDELSE;

    override fun toString() = name
}