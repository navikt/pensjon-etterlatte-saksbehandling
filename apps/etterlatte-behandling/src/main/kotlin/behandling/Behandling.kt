package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.Beregning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.*

data class Behandling(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Behandlingsopplysning<ObjectNode>>,
    val gyldighetsprøving: GyldighetsResultat?,
    val vilkårsprøving: VilkaarResultat?,
    val beregning: Beregning?,
    val fastsatt: Boolean = false,
    val avbrutt: Boolean = false
) {
    val status
        get() = when {
            avbrutt -> BehandlingStatus.AVBRUTT
            fastsatt -> BehandlingStatus.FASTSATT
            beregning != null -> BehandlingStatus.BEREGNET
            vilkårsprøving != null -> BehandlingStatus.VILKÅRSPRØVD
            gyldighetsprøving != null -> BehandlingStatus.GYLDIGHETSPRØVD
            else -> BehandlingStatus.OPPRETTET
        }
}