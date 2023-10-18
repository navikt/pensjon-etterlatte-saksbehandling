package no.nav.etterlatte.libs.common.behandling

import java.util.UUID

data class ForenkletBehandling(
    val sakId: Long,
    val behandlingId: UUID,
    val status: BehandlingStatus,
)

data class ForenkletBehandlingListeWrapper(
    val behandlinger: List<ForenkletBehandling>,
)
