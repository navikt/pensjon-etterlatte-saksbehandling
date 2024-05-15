package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Omregningshendelse(
    val sakId: Long,
    val fradato: LocalDate,
    val omregningsId: UUID? = null,
    val prosesstype: Prosesstype,
    val revurderingaarsak: Revurderingaarsak = Revurderingaarsak.REGULERING,
    val oppgavefrist: LocalDate? = null,
    // TODO kun relevant for regulering 2024 da feltet opphoerFraOgMed er innført i forkant av reguleringen 2024
    val opphoerFraOgMed: YearMonth? = null,
)
