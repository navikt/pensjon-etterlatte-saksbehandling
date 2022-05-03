package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class GrunnlagsBehov(
    val sak: Long,
    val opplysninger: List<Grunnlagsopplysning<ObjectNode>>?
)

data class LeggTilOpplysningerRequest(
    val opplysninger: List<Grunnlagsopplysning<ObjectNode>>
)