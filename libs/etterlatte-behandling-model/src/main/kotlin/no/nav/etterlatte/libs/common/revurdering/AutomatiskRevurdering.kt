package no.nav.etterlatte.libs.common.revurdering

import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

data class AutomatiskRevurderingRequest(
    val sakId: SakId,
    val fraDato: LocalDate,
    val revurderingAarsak: Revurderingaarsak,
    val oppgavefrist: Tidspunkt?,
)

data class AutomatiskRevurderingResponse(
    val behandlingId: UUID,
    val forrigeBehandlingId: UUID,
    val sakType: SakType,
)
