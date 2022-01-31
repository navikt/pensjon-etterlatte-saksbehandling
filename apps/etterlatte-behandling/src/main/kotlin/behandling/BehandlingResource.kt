package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
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

data class BehandlingsBehov(
    val sak: Long,
    val opplysninger: List<Behandlingsopplysning>?
)

