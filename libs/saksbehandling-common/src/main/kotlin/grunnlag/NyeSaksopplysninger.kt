package no.nav.etterlatte.libs.common.grunnlag

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import tools.jackson.databind.JsonNode

data class NyeSaksopplysninger(
    val sakId: SakId,
    val opplysninger: List<Grunnlagsopplysning<JsonNode>>,
)

data class NyePersonopplysninger(
    val sakId: SakId,
    val fnr: Folkeregisteridentifikator,
    val opplysninger: List<Grunnlagsopplysning<JsonNode>>,
)
