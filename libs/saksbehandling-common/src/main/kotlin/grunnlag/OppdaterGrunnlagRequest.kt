package no.nav.etterlatte.libs.common.grunnlag

import no.nav.etterlatte.libs.common.behandling.SakType

data class OppdaterGrunnlagRequest(
    val sakId: Long,
    val sakType: SakType,
)
