package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode

data class BehandlingsBehov(
    val sak: Long,
    //val opplysninger: List<Grunnlagsopplysning<ObjectNode>>? TODO: andre ting her?
)
