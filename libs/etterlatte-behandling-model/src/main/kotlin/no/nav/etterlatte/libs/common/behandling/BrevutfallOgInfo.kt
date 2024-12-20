package no.nav.etterlatte.libs.common.behandling

import java.util.UUID

data class BrevutfallOgInfo(
    val behandlingId: UUID?,
    val brevutfall: BrevutfallDto?,
)
