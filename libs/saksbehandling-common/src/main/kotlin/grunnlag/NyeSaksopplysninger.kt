package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.sak.SakId

data class NyeSaksopplysninger(
    val sakId: SakId,
    val opplysninger: List<Grunnlagsopplysning<JsonNode>>,
)
