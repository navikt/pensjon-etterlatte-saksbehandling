package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.JsonNode

data class NyeSaksopplysninger(
    val opplysninger: List<Grunnlagsopplysning<JsonNode>>
)