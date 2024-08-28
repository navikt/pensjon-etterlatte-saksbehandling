package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate
import java.util.UUID

data class Omregningshendelse(
    val sakId: SakId,
    val fradato: LocalDate,
    val omregningsId: UUID? = null,
    val prosesstype: Prosesstype,
    val revurderingaarsak: Revurderingaarsak = Revurderingaarsak.REGULERING,
    val oppgavefrist: LocalDate? = null,
)
