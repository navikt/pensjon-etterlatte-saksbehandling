package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.revurdering.RevurderingMedBegrunnelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.util.*

data class ManuellRevurdering(
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
    override val begrunnelse: String?
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
    prosesstype = Prosesstype.MANUELL,
    kilde = kilde,
    begrunnelse = begrunnelse
) {

    private fun erFyltUt(): Boolean =
        when (sak.sakType) {
            SakType.BARNEPENSJON -> (virkningstidspunkt != null)
            SakType.OMSTILLINGSSTOENAD -> (virkningstidspunkt != null)
        }

    override fun kopier() = this.copy()

    override fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt) =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET).copy(virkningstidspunkt = virkningstidspunkt) }

    override fun oppdaterUtenlandstilsnitt(utenlandstilsnitt: Utenlandstilsnitt) =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET).copy(utenlandstilsnitt = utenlandstilsnitt) }

    override fun oppdaterBoddEllerArbeidetUtlandnet(boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet)
        }

    override fun tilOpprettet() =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET) }

    override fun tilVilkaarsvurdert(): Revurdering {
        if (!erFyltUt()) {
            logger.info("Behandling ($id) må være fylt ut for å settes til vilkårsvurdert")
            throw TilstandException.IkkeFyltUt
        }

        return hvisRedigerbar { endreTilStatus(BehandlingStatus.VILKAARSVURDERT) }
    }

    override fun tilTrygdetidOppdatert(): Revurdering = hvisTilstandEr(
        listOf(
            BehandlingStatus.VILKAARSVURDERT,
            BehandlingStatus.TRYGDETID_OPPDATERT,
            BehandlingStatus.BEREGNET,
            BehandlingStatus.AVKORTET,
            BehandlingStatus.RETURNERT
        )
    ) { endreTilStatus(BehandlingStatus.TRYGDETID_OPPDATERT) }

    override fun tilBeregnet(fastTrygdetid: Boolean) =
        hvisTilstandEr(
            if (fastTrygdetid) {
                listOf(
                    BehandlingStatus.VILKAARSVURDERT,
                    BehandlingStatus.BEREGNET,
                    BehandlingStatus.RETURNERT
                )
            } else {
                listOf(
                    BehandlingStatus.TRYGDETID_OPPDATERT,
                    BehandlingStatus.BEREGNET,
                    BehandlingStatus.RETURNERT
                )
            }
        ) { endreTilStatus(BehandlingStatus.BEREGNET) }

    override fun tilAvkortet() = hvisTilstandEr(
        listOf(
            BehandlingStatus.BEREGNET,
            BehandlingStatus.AVKORTET,
            BehandlingStatus.RETURNERT
        )
    ) { endreTilStatus(BehandlingStatus.AVKORTET) }

    override fun tilFattetVedtak(): Revurdering {
        if (!erFyltUt()) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til fattet vedtak"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisTilstandEr(
            listOf(
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT
            )
        ) {
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