package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode

data class NyeSaksopplysninger(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val opplysninger: List<Grunnlagsopplysning<JsonNode>>,
)
