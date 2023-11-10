package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.util.UUID

data class Foerstegangsbehandling(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    override val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val soeknadMottattDato: LocalDateTime?,
    val gyldighetsproeving: GyldighetsResultat?,
    override val prosesstype: Prosesstype = Prosesstype.MANUELL,
    override val kilde: Vedtaksloesning,
) : Behandling() {
    override val type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING

    private fun erFyltUt(): Boolean =
        when (sak.sakType) {
            SakType.BARNEPENSJON ->
                (virkningstidspunkt != null) && (gyldighetsproeving != null) && (kommerBarnetTilgode != null)

            SakType.OMSTILLINGSSTOENAD ->
                (virkningstidspunkt != null) && (gyldighetsproeving != null)
        }

    fun oppdaterGyldighetsproeving(gyldighetsResultat: GyldighetsResultat): Foerstegangsbehandling =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(gyldighetsproeving = gyldighetsResultat)
        }

    override fun oppdaterVirkningstidspunkt(virkningstidspunkt: Virkningstidspunkt) =
        hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET).copy(virkningstidspunkt = virkningstidspunkt) }

    override fun oppdaterBoddEllerArbeidetUtlandnet(boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet)
        }

    override fun tilOpprettet(): Foerstegangsbehandling {
        return hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET) }
    }

    override fun tilVilkaarsvurdert(): Foerstegangsbehandling {
        if (!erFyltUt()) {
            logger.info("Behandling ($id) må være fylt ut for å settes til vilkårsvurdert")
            throw TilstandException.IkkeFyltUt
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
        ) { endreTilStatus(BehandlingStatus.TRYGDETID_OPPDATERT) }

    override fun tilBeregnet(fastTrygdetid: Boolean): Foerstegangsbehandling =
        hvisTilstandEr(
            if (fastTrygdetid) {
                listOf(
                    BehandlingStatus.VILKAARSVURDERT,
                    BehandlingStatus.BEREGNET,
                    BehandlingStatus.AVKORTET,
                    BehandlingStatus.RETURNERT,
                )
            } else {
                listOf(
                    BehandlingStatus.TRYGDETID_OPPDATERT,
                    BehandlingStatus.BEREGNET,
                    BehandlingStatus.AVKORTET,
                    BehandlingStatus.RETURNERT,
                )
            },
        ) { endreTilStatus(BehandlingStatus.BEREGNET) }

    override fun tilAvkortet(): Foerstegangsbehandling =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
        ) { endreTilStatus(BehandlingStatus.AVKORTET) }

    override fun tilFattetVedtak(): Foerstegangsbehandling {
        if (!erFyltUt()) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til fattet vedtak"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisTilstandEr(
            listOf(
                BehandlingStatus.VILKAARSVURDERT, // TODO EY-2927
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
        ) {
            endreTilStatus(BehandlingStatus.FATTET_VEDTAK)
        }
    }

    override fun tilAttestert() =
        hvisTilstandEr(BehandlingStatus.FATTET_VEDTAK) {
            endreTilStatus(BehandlingStatus.ATTESTERT)
        }

    override fun tilReturnert() =
        hvisTilstandEr(BehandlingStatus.FATTET_VEDTAK) {
            endreTilStatus(BehandlingStatus.RETURNERT)
        }

    override fun tilTilSamordning() =
        hvisTilstandEr(listOf(BehandlingStatus.ATTESTERT)) {
            endreTilStatus(BehandlingStatus.TIL_SAMORDNING)
        }

    override fun tilSamordnet() =
        hvisTilstandEr(listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.TIL_SAMORDNING)) {
            endreTilStatus(BehandlingStatus.SAMORDNET)
        }

    override fun tilIverksatt() =
        hvisTilstandEr(listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.SAMORDNET)) {
            endreTilStatus(BehandlingStatus.IVERKSATT)
        }

    private fun endreTilStatus(status: BehandlingStatus) = this.copy(status = status, sistEndret = Tidspunkt.now().toLocalDatetimeUTC())
}
