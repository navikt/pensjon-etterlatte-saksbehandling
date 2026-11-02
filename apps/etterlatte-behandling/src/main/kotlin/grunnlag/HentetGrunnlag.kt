package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import tools.jackson.databind.JsonNode

data class HentetGrunnlag(
    val personopplysninger: List<Pair<Folkeregisteridentifikator, List<Grunnlagsopplysning<JsonNode>>>>,
    val saksopplysninger: List<Grunnlagsopplysning<JsonNode>>,
)
