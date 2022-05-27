package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode

data class Grunnlag(
    val saksId: Long,
    val grunnlag: List<Grunnlagsopplysning<ObjectNode>>,
    val versjon: Long
)