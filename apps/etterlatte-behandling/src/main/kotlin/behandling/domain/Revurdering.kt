package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDateTime
import java.util.*

data class Revurdering(
    override val id: UUID,
    override val sak: Long,
    override val sakType: SakType,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val vilkaarUtfall: VilkaarsvurderingUtfall?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    val revurderingsaarsak: RevurderingAarsak
) : Behandling() {
    override val type: BehandlingType = BehandlingType.REVURDERING
}