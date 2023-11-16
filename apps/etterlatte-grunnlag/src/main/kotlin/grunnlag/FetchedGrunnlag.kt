package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

data class FetchedGrunnlag(
    val personopplysninger: List<Pair<Folkeregisteridentifikator, List<Grunnlagsopplysning<JsonNode>>>>,
    val saksopplysninger: List<Grunnlagsopplysning<JsonNode>>,
)
