package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning

/*
data class BehandlingSammendragListe(val behandlinger: List<BehandlingSammendrag>)
data class BehandlingSammendrag(val id: UUID, val sak: Long, val status: BehandlingStatus)
data class DetaljertBehandling(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Behandlingsopplysning>,
    val vilkårsprøving: Vilkårsprøving?,
    val beregning: Beregning?,
    val fastsatt: Boolean = false
)*/


data class BehandlingsBehov(
    val sak: Long,
    val opplysninger: List<Behandlingsopplysning<ObjectNode>>?
)

