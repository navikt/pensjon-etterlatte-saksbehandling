package no.nav.etterlatte.libs.common.grunnlag

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId

data class OppdaterGrunnlagRequest(
    val sakId: SakId,
    val sakType: SakType,
)
