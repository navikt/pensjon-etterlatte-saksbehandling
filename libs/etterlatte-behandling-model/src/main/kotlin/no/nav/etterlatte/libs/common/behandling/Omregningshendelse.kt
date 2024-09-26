package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate

data class Omregningshendelse(
    val sakId: SakId,
    val fradato: LocalDate,
    val revurderingaarsak: Revurderingaarsak = Revurderingaarsak.REGULERING,
)
