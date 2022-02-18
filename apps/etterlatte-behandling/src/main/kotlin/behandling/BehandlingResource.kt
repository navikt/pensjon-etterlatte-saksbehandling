package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning

data class BehandlingsBehov(
    val sak: Long,
    val opplysninger: List<Behandlingsopplysning<ObjectNode>>?
)

data class LeggTilOpplysningerRequest(
    val opplysninger: List<Behandlingsopplysning<ObjectNode>>
)