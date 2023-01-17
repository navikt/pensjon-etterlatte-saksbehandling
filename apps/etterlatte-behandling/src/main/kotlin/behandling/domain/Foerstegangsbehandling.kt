package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class Foerstegangsbehandling(
    override val id: UUID,
    override val sak: Long,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val kommerBarnetTilgode: KommerBarnetTilgode?,
    override val vilkaarUtfall: VilkaarsvurderingUtfall?,
    override val virkningstidspunkt: Virkningstidspunkt?,
    val soeknadMottattDato: LocalDateTime,
    val gyldighetsproeving: GyldighetsResultat?
) : Behandling() {
    override val type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING

    private val erFyltUt: Boolean
        get() {
            return (virkningstidspunkt != null) && (gyldighetsproeving != null) && (kommerBarnetTilgode != null)
        }

    fun oppdaterGyldighetsproeving(gyldighetsResultat: GyldighetsResultat): Foerstegangsbehandling = hvisRedigerbar {
        endreTilStatus(BehandlingStatus.OPPRETTET).copy(gyldighetsproeving = gyldighetsResultat)
    }

    fun oppdaterVirkningstidspunkt(dato: YearMonth, kilde: Grunnlagsopplysning.Saksbehandler) =
        hvisRedigerbar {
            endreTilStatus(BehandlingStatus.OPPRETTET).copy(virkningstidspunkt = Virkningstidspunkt(dato, kilde))
        }

    fun oppdaterKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode): Foerstegangsbehandling = hvisRedigerbar {
        endreTilStatus(BehandlingStatus.OPPRETTET).copy(kommerBarnetTilgode = kommerBarnetTilgode)
    }

    override fun tilOpprettet(): Foerstegangsbehandling {
        return hvisRedigerbar { endreTilStatus(BehandlingStatus.OPPRETTET) }.copy(vilkaarUtfall = null)
    }

    override fun tilVilkaarsvurdert(utfall: VilkaarsvurderingUtfall?): Foerstegangsbehandling {
        if (!erFyltUt) {
            logger.info("Behandling ($id) må være fylt ut for å settes til vilkårsvurdert")
            throw TilstandException.IkkeFyltUt
        }

        return hvisRedigerbar { endreTilStatus(BehandlingStatus.VILKAARSVURDERT) }.copy(vilkaarUtfall = utfall)
    }

    override fun tilBeregnet(): Foerstegangsbehandling = hvisTilstandEr(
        listOf(
            BehandlingStatus.VILKAARSVURDERT,
            BehandlingStatus.BEREGNET,
            BehandlingStatus.RETURNERT
        )
    ) {
        endreTilStatus(BehandlingStatus.BEREGNET)
    }

    override fun tilFattetVedtak(): Foerstegangsbehandling {
        if (!erFyltUt) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til fattet vedtak"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisTilstandEr(
            listOf(
                BehandlingStatus.BEREGNET,
                BehandlingStatus.VILKAARSVURDERT,
                BehandlingStatus.RETURNERT
            )
        ) {
            require(vilkaarUtfall != null)
            if (status == BehandlingStatus.VILKAARSVURDERT) {
                require(vilkaarUtfall == VilkaarsvurderingUtfall.IKKE_OPPFYLT)
            }

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

    /* TODO ai: Legg denne på Behandling-klassen når flere behandlingstyper skal støtte ny behandlingsflyt */
    private fun endreTilStatus(status: BehandlingStatus) =
        this.copy(status = status, sistEndret = LocalDateTime.now())
}