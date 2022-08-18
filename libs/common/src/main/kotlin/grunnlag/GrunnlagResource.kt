package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode

data class DetaljertGrunnlag(
    val sak: Long,
    val grunnlag: List<Grunnlagsopplysning<ObjectNode>>
)