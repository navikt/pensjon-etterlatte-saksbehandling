package no.nav.etterlatte.libs.common.behandling

import java.util.UUID

data class BrevutfallOgEtterbetalingDto(
    val behandlingId: UUID?,
    val etterbetaling: EtterbetalingDto?,
    val brevutfall: BrevutfallDto?,
)
