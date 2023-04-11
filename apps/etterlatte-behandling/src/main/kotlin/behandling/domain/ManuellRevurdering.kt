package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDateTime
import java.util.*

data class ManuellRevurdering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val vilkaarUtfall: VilkaarsvurderingUtfall?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    override val revurderingsaarsak: RevurderingAarsak
) : Revurdering(
    id,
    sak,
    behandlingOpprettet,
    sistEndret,
    status,
    persongalleri,
    kommerBarnetTilgode,
    vilkaarUtfall,
    virkningstidspunkt,
    revurderingsaarsak,
    Prosesstype.MANUELL
) {

    private fun erFyltUt(): Boolean =
        when (sak.sakType) {
            SakType.BARNEPENSJON -> (virkningstidspunkt != null) && (kommerBarnetTilgode != null)
            SakType.OMSTILLINGSSTOENAD -> (virkningstidspunkt != null)
        }

    override fun kopier() = this.copy()

    override fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt) =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET).copy(virkningstidspunkt = virkningstidspunkt) }

    override fun tilOpprettet() =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET) }.copy(vilkaarUtfall = null)

    override fun tilVilkaarsvurdert(utfall: VilkaarsvurderingUtfall?): Revurdering {
        if (!erFyltUt()) {
            logger.info("Behandling ($id) må være fylt ut for å settes til vilkårsvurdert")
            throw TilstandException.IkkeFyltUt
        }

        return hvisRedigerbar { endreTilStatus(BehandlingStatus.VILKAARSVURDERT) }.copy(vilkaarUtfall = utfall)
    }

    override fun tilBeregnet() = hvisTilstandEr(
        listOf(
            BehandlingStatus.VILKAARSVURDERT,
            BehandlingStatus.BEREGNET,
            BehandlingStatus.RETURNERT
        )
    ) { endreTilStatus(BehandlingStatus.BEREGNET) }

    override fun tilFattetVedtak(): Revurdering {
        if (!erFyltUt()) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til fattet vedtak"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisTilstandEr(listOf(BehandlingStatus.BEREGNET, BehandlingStatus.RETURNERT)) {
            endreTilStatus(BehandlingStatus.FATTET_VEDTAK)
        }
    }

    override fun tilAttestert() = hvisTilstandEr(BehandlingStatus.FATTET_VEDTAK) {
        endreTilStatus(BehandlingStatus.ATTESTERT)
    }

    override fun tilReturnert() = hvisTilstandEr(BehandlingStatus.FATTET_VEDTAK) {
        endreTilStatus(BehandlingStatus.RETURNERT)
    }

    override fun tilIverksatt() = hvisTilstandEr(BehandlingStatus.ATTESTERT) {
        endreTilStatus(BehandlingStatus.IVERKSATT)
    }

    private fun endreTilStatus(status: BehandlingStatus) =
        this.copy(status = status, sistEndret = Tidspunkt.now().toLocalDatetimeUTC())
}