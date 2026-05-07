package no.nav.etterlatte.brev

import no.nav.etterlatte.libs.common.sak.SakId

data class BehandlingsvurderingNotatRequest(
    val sakId: SakId,
    val slate: Slate,
)
