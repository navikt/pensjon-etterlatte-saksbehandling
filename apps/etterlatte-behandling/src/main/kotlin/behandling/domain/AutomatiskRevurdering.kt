package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.revurdering.RevurderingMedBegrunnelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.util.UUID

data class AutomatiskRevurdering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    override val utenlandstilsnitt: Utenlandstilsnitt?,
    override val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    override val revurderingsaarsak: RevurderingAarsak,
    override val revurderingInfo: RevurderingMedBegrunnelse?,
    override val kilde: Vedtaksloesning,
    override val begrunnelse: String?,
) : Revurdering(
        id = id,
        sak = sak,
        behandlingOpprettet = behandlingOpprettet,
        sistEndret = sistEndret,
        status = status,
        kommerBarnetTilgode = kommerBarnetTilgode,
        virkningstidspunkt = virkningstidspunkt,
        utenlandstilsnitt = utenlandstilsnitt,
        boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
        revurderingsaarsak = revurderingsaarsak,
        revurderingInfo = revurderingInfo,
        prosesstype = Prosesstype.AUTOMATISK,
        kilde = kilde,
        begrunnelse = begrunnelse,
    ) {
    override fun kopier() = this.copy()

    override fun tilOpprettet() = endreTilStatus(BehandlingStatus.OPPRETTET)

    override fun tilVilkaarsvurdert() = endreTilStatus(BehandlingStatus.VILKAARSVURDERT)

    override fun tilTrygdetidOppdatert() = endreTilStatus(BehandlingStatus.TRYGDETID_OPPDATERT)

    override fun tilBeregnet(fastTrygdetid: Boolean) = endreTilStatus(BehandlingStatus.BEREGNET)

    override fun tilAvkortet() = endreTilStatus(BehandlingStatus.AVKORTET)

    override fun tilFattetVedtak() = endreTilStatus(BehandlingStatus.FATTET_VEDTAK)

    override fun tilAttestert() = endreTilStatus(BehandlingStatus.ATTESTERT)

    override fun tilReturnert() = endreTilStatus(BehandlingStatus.RETURNERT)

    override fun tilIverksatt() = endreTilStatus(BehandlingStatus.IVERKSATT)

    private fun endreTilStatus(status: BehandlingStatus) = this.copy(status = status, sistEndret = Tidspunkt.now().toLocalDatetimeUTC())
}
