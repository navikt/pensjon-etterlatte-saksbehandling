package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.Beregning
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.util.*

data class Behandling(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Behandlingsopplysning<ObjectNode>>,
    val vilkårsprøving: List<VurdertVilkaar>?,
    val beregning: Beregning?,
    val fastsatt: Boolean = false
) {
    val status
        get() = when {
            fastsatt -> BehandlingStatus.FASTSATT
            beregning != null -> BehandlingStatus.BEREGNET
            vilkårsprøving != null -> BehandlingStatus.VILKÅRSPRØVD
            else -> BehandlingStatus.OPPRETTET
        }
}