package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Foerstegangsbehandling(
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
    val gyldighetsproeving: GyldighetsResultat?,
    override val prosesstype: Prosesstype = Prosesstype.MANUELL,
    override val vedtaksloesning: Vedtaksloesning,
    override val sendeBrev: Boolean,
    override val opphoerFraOgMed: YearMonth? = null,
    override val tidligereFamiliepleier: TidligereFamiliepleier? = null,
    val erSluttbehandling: Boolean = false,
    override val relatertBehandlingId: String? = null,
    override val opprinnelse: BehandlingOpprinnelse = BehandlingOpprinnelse.UKJENT,
) : Behandling() {
    override val type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING

    private fun erFyltUt(): Boolean =
        when (sak.sakType) {
            SakType.BARNEPENSJON ->
                (virkningstidspunkt != null) && (gyldighetsproeving != null) && (kommerBarnetTilgode != null)

            SakType.OMSTILLINGSSTOENAD ->
                (virkningstidspunkt != null) && (gyldighetsproeving != null)
        }

    override fun erSluttbehandling(): Boolean = this.erSluttbehandling

    fun oppdaterGyldighetsproeving(gyldighetsResultat: GyldighetsResultat): Foerstegangsbehandling =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(gyldighetsproeving = gyldighetsResultat)
        }

    override fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt) =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET).copy(virkningstidspunkt = virkningstidspunkt) }

    override fun oppdaterUtlandstilknytning(utlandstilknytning: Utlandstilknytning) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(utlandstilknytning = utlandstilknytning)
        }

    override fun oppdaterViderefoertOpphoer(viderefoertOpphoer: ViderefoertOpphoer?) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(opphoerFraOgMed = viderefoertOpphoer?.dato)
        }

    override fun oppdaterBoddEllerArbeidetUtlandet(boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet)
        }

    override fun oppdaterTidligereFamiliepleier(tidligereFamiliepleier: TidligereFamiliepleier) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(tidligereFamiliepleier = tidligereFamiliepleier)
        }

    override fun tilOpprettet(): Foerstegangsbehandling = hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET) }

    override fun tilVilkaarsvurdert(): Foerstegangsbehandling {
        if (!erFyltUt()) {
            throw TilstandException.IkkeFyltUt(
                "Behandling ($id) må ha vurdert gyldig framsatt, " +
                    "virkningstidspunkt og kommer bruker til gode for å settes til fattet vedtak",
            )
        }

        return hvisRedigerbar { endreTilStatus(BehandlingStatus.VILKAARSVURDERT) }
    }

    override fun tilTrygdetidOppdatert(): Foerstegangsbehandling =
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

    override fun tilBeregnet(): Foerstegangsbehandling =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.TRYGDETID_OPPDATERT,
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.BEREGNET,
        ) { endreTilStatus(it) }

    override fun tilAvkortet(): Foerstegangsbehandling =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.AVKORTET,
        ) { endreTilStatus(it) }

    override fun tilFattetVedtak(): Foerstegangsbehandling {
        if (!erFyltUt()) {
            throw TilstandException.IkkeFyltUt(
                "Behandling ($id) må ha vurdert gyldig framsatt, " +
                    "virkningstidspunkt og kommer bruker til gode for å settes til fattet vedtak",
            )
        }

        return hvisTilstandEr(
            listOf(
                // TODO EY-2927
                BehandlingStatus.VILKAARSVURDERT,
                BehandlingStatus.TRYGDETID_OPPDATERT,
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
