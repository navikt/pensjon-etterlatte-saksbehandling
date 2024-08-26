package no.nav.etterlatte.libs.common.grunnlag

import no.nav.etterlatte.libs.common.behandling.SakType

data class OppdaterGrunnlagRequest(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val sakType: SakType,
)
