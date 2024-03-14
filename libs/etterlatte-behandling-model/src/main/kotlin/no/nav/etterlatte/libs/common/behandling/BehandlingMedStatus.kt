package no.nav.etterlatte.libs.common.behandling

import java.util.UUID

data class BehandlingMedStatus(
    val id: UUID,
    val status: BehandlingStatus,
    val ident: String?,
)
