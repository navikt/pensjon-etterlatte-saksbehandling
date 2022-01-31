package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import java.util.*

data class Behandling(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Behandlingsopplysning>,
    val vilkårsprøving: Vilkårsprøving?,
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