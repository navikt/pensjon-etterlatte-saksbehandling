package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.* //ktlint-disable
import no.nav.etterlatte.libs.common.behandling.BehandlingType
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

data class Revurdering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val vilkaarUtfall: VilkaarsvurderingUtfall?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    val revurderingsaarsak: RevurderingAarsak,
    val prosesstype: Prosesstype
) : Behandling() {
    override val type: BehandlingType = BehandlingType.REVURDERING

    private fun erFyltUt(): Boolean =
        when (sak.sakType) {
            SakType.BARNEPENSJON -> (virkningstidspunkt != null) && (kommerBarnetTilgode != null)
            SakType.OMSTILLINGSSTOENAD -> (virkningstidspunkt != null)
        }

    override fun tilOpprettet(): Revurdering {
        if (prosesstype == Prosesstype.AUTOMATISK) {
            return endreTilStatus(OPPRETTET).copy(vilkaarUtfall = null)
        }

        return hvisRedigerbar { endreTilStatus(OPPRETTET) }.copy(vilkaarUtfall = null)
    }

    override fun tilVilkaarsvurdert(utfall: VilkaarsvurderingUtfall?): Revurdering {
        if (prosesstype == Prosesstype.AUTOMATISK) {
            return endreTilStatus(VILKAARSVURDERT).copy(vilkaarUtfall = utfall)
        }

        if (!erFyltUt()) {
            logger.info("Behandling ($id) må være fylt ut for å settes til vilkårsvurdert")
            throw TilstandException.IkkeFyltUt
        }

        return hvisRedigerbar { endreTilStatus(VILKAARSVURDERT) }.copy(vilkaarUtfall = utfall)
    }

    override fun tilBeregnet(): Revurdering {
        if (prosesstype == Prosesstype.AUTOMATISK) {
            return endreTilStatus(BEREGNET)
        }

        return hvisTilstandEr(listOf(VILKAARSVURDERT, BEREGNET, RETURNERT)) { endreTilStatus(BEREGNET) }
    }

    override fun tilFattetVedtak(): Revurdering {
        if (prosesstype == Prosesstype.AUTOMATISK) {
            return endreTilStatus(FATTET_VEDTAK)
        }

        if (!erFyltUt()) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til fattet vedtak"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisTilstandEr(listOf(BEREGNET, RETURNERT)) {
            endreTilStatus(FATTET_VEDTAK)
        }
    }

    override fun tilAttestert(): Revurdering {
        if (prosesstype == Prosesstype.AUTOMATISK) {
            return endreTilStatus(ATTESTERT)
        }

        return hvisTilstandEr(FATTET_VEDTAK) {
            endreTilStatus(ATTESTERT)
        }
    }

    override fun tilReturnert(): Revurdering {
        if (prosesstype == Prosesstype.AUTOMATISK) {
            return endreTilStatus(RETURNERT)
        }

        return hvisTilstandEr(FATTET_VEDTAK) {
            endreTilStatus(RETURNERT)
        }
    }

    override fun tilIverksatt(): Revurdering {
        if (prosesstype == Prosesstype.AUTOMATISK) {
            return endreTilStatus(IVERKSATT)
        }

        return hvisTilstandEr(ATTESTERT) {
            endreTilStatus(IVERKSATT)
        }
    }

    private fun endreTilStatus(status: BehandlingStatus) =
        this.copy(status = status, sistEndret = Tidspunkt.now().toLocalDatetimeUTC())
}