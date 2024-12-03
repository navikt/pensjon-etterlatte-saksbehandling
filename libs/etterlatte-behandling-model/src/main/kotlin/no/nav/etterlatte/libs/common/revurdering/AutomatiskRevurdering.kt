package no.nav.etterlatte.libs.common.revurdering

import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AutomatiskRevurderingRequest(
    val sakId: SakId,
    val fraDato: LocalDate,
    val revurderingAarsak: Revurderingaarsak,
    val oppgavefrist: LocalDate? = null,
    val mottattDato: LocalDateTime? = null,
)

data class AutomatiskRevurderingResponse(
    val behandlingId: UUID,
    val forrigeBehandlingId: UUID,
    val sakType: SakType,
)
