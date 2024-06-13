package no.nav.etterlatte.libs.common.omregning

import no.nav.etterlatte.libs.common.behandling.SakType
import java.util.UUID

data class OpprettOmregningResponse(
    val behandlingId: UUID,
    val forrigeBehandlingId: UUID,
    val sakType: SakType,
)
