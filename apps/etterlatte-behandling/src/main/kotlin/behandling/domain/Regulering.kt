package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDateTime
import java.util.UUID

data class Regulering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val vilkaarUtfall: VilkaarsvurderingUtfall?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    val revurderingsaarsak: RevurderingAarsak = RevurderingAarsak.GRUNNBELOEPREGULERING,
    val prosesstype: Prosesstype
) : Behandling() {
    override val type: BehandlingType = BehandlingType.OMREGNING

    override fun tilOpprettet() = endreTilStatus(BehandlingStatus.OPPRETTET)

    override fun tilVilkaarsvurdert(utfall: VilkaarsvurderingUtfall?) = endreTilStatus(BehandlingStatus.VILKAARSVURDERT)

    override fun tilBeregnet() = endreTilStatus(BehandlingStatus.BEREGNET)

    override fun tilFattetVedtak() = endreTilStatus(BehandlingStatus.FATTET_VEDTAK)

    override fun tilAttestert() = endreTilStatus(BehandlingStatus.ATTESTERT)

    override fun tilReturnert() = endreTilStatus(BehandlingStatus.RETURNERT)

    override fun tilIverksatt() = endreTilStatus(BehandlingStatus.IVERKSATT)

    private fun endreTilStatus(status: BehandlingStatus) =
        this.copy(status = status, sistEndret = Tidspunkt.now().toLocalDatetimeUTC())
}