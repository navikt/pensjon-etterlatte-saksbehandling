package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt

data class VilkaarsvurderingBehandlingDTO(
    val sak: Long,
    val sakType: SakType,
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?
) {
    companion object {
        fun fra(detaljertBehandling: DetaljertBehandling): VilkaarsvurderingBehandlingDTO =
            VilkaarsvurderingBehandlingDTO(
                sak = detaljertBehandling.sak,
                sakType = detaljertBehandling.sakType,
                behandlingType = detaljertBehandling.behandlingType,
                virkningstidspunkt = detaljertBehandling.virkningstidspunkt
            )
    }
}