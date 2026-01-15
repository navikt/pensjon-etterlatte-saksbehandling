package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class ManuellRevurdering(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    override val utlandstilknytning: Utlandstilknytning?,
    override val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    override val soeknadMottattDato: LocalDateTime?,
    override val revurderingsaarsak: Revurderingaarsak,
    override val revurderingInfo: RevurderingInfoMedBegrunnelse?,
    override val vedtaksloesning: Vedtaksloesning,
    override val opprinnelse: BehandlingOpprinnelse,
    override val begrunnelse: String?,
    override val relatertBehandlingId: String?,
    override val sendeBrev: Boolean,
    override val opphoerFraOgMed: YearMonth?,
    override val tidligereFamiliepleier: TidligereFamiliepleier?,
) : Revurdering(
        id = id,
        sak = sak,
        behandlingOpprettet = behandlingOpprettet,
        sistEndret = sistEndret,
        status = status,
        kommerBarnetTilgode = kommerBarnetTilgode,
        virkningstidspunkt = virkningstidspunkt,
        boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
        soeknadMottattDato = soeknadMottattDato,
        revurderingsaarsak = revurderingsaarsak,
        revurderingInfo = revurderingInfo,
        prosesstype = Prosesstype.MANUELL,
        vedtaksloesning = vedtaksloesning,
        opprinnelse = opprinnelse,
        begrunnelse = begrunnelse,
        relatertBehandlingId = relatertBehandlingId,
        opphoerFraOgMed = opphoerFraOgMed,
        tidligereFamiliepleier = tidligereFamiliepleier,
    ) {
    private fun erFyltUt(): Boolean =
        when (sak.sakType) {
            SakType.BARNEPENSJON -> (virkningstidspunkt != null)
            SakType.OMSTILLINGSSTOENAD -> (virkningstidspunkt != null)
        }

    override fun kopier() = this.copy()

    override fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt) =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET).copy(virkningstidspunkt = virkningstidspunkt) }

    override fun oppdaterBoddEllerArbeidetUtlandet(boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet)
        }

    override fun oppdaterUtlandstilknytning(utlandstilknytning: Utlandstilknytning) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(utlandstilknytning = utlandstilknytning)
        }

    override fun oppdaterViderefoertOpphoer(viderefoertOpphoer: ViderefoertOpphoer?) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(opphoerFraOgMed = viderefoertOpphoer?.dato)
        }

    override fun oppdaterTidligereFamiliepleier(tidligereFamiliepleier: TidligereFamiliepleier) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(tidligereFamiliepleier = tidligereFamiliepleier)
        }

    override fun tilOpprettet() = hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET) }

    override fun tilVilkaarsvurdert(): Revurdering {
        if (!erFyltUt()) {
            throw TilstandException.IkkeFyltUt(
                "Behandling ($id) må ha satt virkningstidspunkt " +
                    "for å settes til vilkårsvurdert",
            )
        }

        return hvisRedigerbar { endreTilStatus(BehandlingStatus.VILKAARSVURDERT) }
    }

    override fun tilAttestertIngenEndring(): Revurdering =
        hvisTilstandEr(
            listOf(BehandlingStatus.FATTET_VEDTAK),
            BehandlingStatus.ATTESTERT_INGEN_ENDRING,
        ) { endreTilStatus(it) }

    override fun tilTrygdetidOppdatert(): Revurdering =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.VILKAARSVURDERT,
                BehandlingStatus.TRYGDETID_OPPDATERT,
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.TRYGDETID_OPPDATERT,
        ) { endreTilStatus(it) }

    override fun tilBeregnet() =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.TRYGDETID_OPPDATERT,
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.BEREGNET,
        ) { endreTilStatus(it) }

    override fun tilAvkortet() =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.AVKORTET,
        ) { endreTilStatus(it) }

    /**
     Utforskning av mulighet for vilkaarsvurdert -> fattet_vedtak i kontekst av opphør
     */
    fun tilFattetVedtakUtvidet(): Revurdering {
        if (!erFyltUt()) {
            throw TilstandException.IkkeFyltUt(
                "Behandling ($id) må ha satt virkningstidspunkt " +
                    "for å settes til fattet vedtak",
            )
        }

        return hvisTilstandEr(
            listOf(
                BehandlingStatus.VILKAARSVURDERT,
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
                BehandlingStatus.TRYGDETID_OPPDATERT,
            ),
            BehandlingStatus.FATTET_VEDTAK,
        ) {
            endreTilStatus(it)
        }
    }

    override fun tilFattetVedtak(): Revurdering {
        if (!erFyltUt()) {
            throw TilstandException.IkkeFyltUt(
                "Behandling ($id) må ha satt virkningstidspunkt " +
                    "for å settes til fattet vedtak",
            )
        }

        return hvisTilstandEr(
            listOf(
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.FATTET_VEDTAK,
        ) {
            endreTilStatus(it)
        }
    }

    override fun tilAttestert() =
        hvisTilstandEr(BehandlingStatus.FATTET_VEDTAK, BehandlingStatus.ATTESTERT) {
            endreTilStatus(it)
        }

    override fun tilAvslag() =
        hvisTilstandEr(BehandlingStatus.FATTET_VEDTAK, BehandlingStatus.AVSLAG) {
            endreTilStatus(it)
        }

    override fun tilReturnert() =
        hvisTilstandEr(BehandlingStatus.FATTET_VEDTAK, BehandlingStatus.RETURNERT) {
            endreTilStatus(it)
        }

    override fun tilTilSamordning() =
        hvisTilstandEr(listOf(BehandlingStatus.ATTESTERT), BehandlingStatus.TIL_SAMORDNING) {
            endreTilStatus(it)
        }

    override fun tilSamordnet() =
        hvisTilstandEr(
            listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.TIL_SAMORDNING),
            BehandlingStatus.SAMORDNET,
        ) {
            endreTilStatus(it)
        }

    override fun tilIverksatt() =
        hvisTilstandEr(listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.SAMORDNET), BehandlingStatus.IVERKSATT) {
            endreTilStatus(it)
        }

    private fun endreTilStatus(status: BehandlingStatus) = this.copy(status = status, sistEndret = Tidspunkt.now().toLocalDatetimeUTC())
}
