package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class AutomatiskRevurdering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    override val utlandstilknytning: Utlandstilknytning?,
    override val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    override val revurderingsaarsak: Revurderingaarsak,
    override val revurderingInfo: RevurderingInfoMedBegrunnelse?,
    override val kilde: Vedtaksloesning,
    override val begrunnelse: String?,
    override val relatertBehandlingId: String?,
    override val sendeBrev: Boolean,
    override val opphoerFraOgMed: YearMonth? = null,
) : Revurdering(
        id = id,
        sak = sak,
        behandlingOpprettet = behandlingOpprettet,
        sistEndret = sistEndret,
        status = status,
        kommerBarnetTilgode = kommerBarnetTilgode,
        virkningstidspunkt = virkningstidspunkt,
        boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
        revurderingsaarsak = revurderingsaarsak,
        revurderingInfo = revurderingInfo,
        prosesstype = Prosesstype.AUTOMATISK,
        kilde = kilde,
        begrunnelse = begrunnelse,
        relatertBehandlingId = relatertBehandlingId,
        opphoerFraOgMed = opphoerFraOgMed,
    ) {
    override fun kopier() = this.copy()

    override fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt) =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET).copy(virkningstidspunkt = virkningstidspunkt) }

    override fun oppdaterVidereførtOpphoer(viderefoertOpphoer: ViderefoertOpphoer) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(opphoerFraOgMed = viderefoertOpphoer.dato)
        }

    override fun tilOpprettet() = endreTilStatus(BehandlingStatus.OPPRETTET)

    override fun tilVilkaarsvurdert() = endreTilStatus(BehandlingStatus.VILKAARSVURDERT)

    override fun tilTrygdetidOppdatert() = endreTilStatus(BehandlingStatus.TRYGDETID_OPPDATERT)

    override fun tilBeregnet() = endreTilStatus(BehandlingStatus.BEREGNET)

    override fun tilAvkortet() = endreTilStatus(BehandlingStatus.AVKORTET)

    override fun tilFattetVedtak() = endreTilStatus(BehandlingStatus.FATTET_VEDTAK)

    override fun tilAttestert() = endreTilStatus(BehandlingStatus.ATTESTERT)

    override fun tilAvslag() = endreTilStatus(BehandlingStatus.AVSLAG)

    override fun tilReturnert() = endreTilStatus(BehandlingStatus.RETURNERT)

    override fun tilTilSamordning() = endreTilStatus(BehandlingStatus.TIL_SAMORDNING)

    override fun tilSamordnet() = endreTilStatus(BehandlingStatus.SAMORDNET)

    override fun tilIverksatt() = endreTilStatus(BehandlingStatus.IVERKSATT)

    private fun endreTilStatus(status: BehandlingStatus) = this.copy(status = status, sistEndret = Tidspunkt.now().toLocalDatetimeUTC())
}
