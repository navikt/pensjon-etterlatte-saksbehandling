package no.nav.etterlatte.libs.common.behandling

import java.util.*

data class BehandlingSammendragListe(val behandlinger: List<BehandlingSammendrag>)
data class BehandlingSammendrag(val id: UUID, val sak: Long, val status: BehandlingStatus)
data class DetaljertBehandling(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Behandlingsopplysning>,
    val vilkårsprøving: Vilkårsprøving?,
    val beregning: Beregning?,
    val fastsatt: Boolean = false
)