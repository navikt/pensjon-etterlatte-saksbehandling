package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.util.UUID

data class ManueltOpphoer(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val virkningstidspunkt: Virkningstidspunkt?,
    override val utlandstilknytning: Utlandstilknytning?,
    override val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val opphoerAarsaker: List<String>,
    val fritekstAarsak: String?,
    override val prosesstype: Prosesstype = Prosesstype.MANUELL,
) : Behandling() {
    override val type: BehandlingType = BehandlingType.MANUELT_OPPHOER
    override val kilde = Vedtaksloesning.GJENNY

    private val erFyltUt: Boolean
        get() {
            return (virkningstidspunkt != null)
        }

    override val kommerBarnetTilgode: KommerBarnetTilgode?
        get() = null

    override fun tilBeregnet(): ManueltOpphoer =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.BEREGNET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.BEREGNET,
        ) {
            endreTilStatus(it)
        }

    override fun tilAvkortet(): ManueltOpphoer =
        hvisTilstandEr(
            listOf(
                BehandlingStatus.BEREGNET,
                BehandlingStatus.AVKORTET,
                BehandlingStatus.RETURNERT,
            ),
            BehandlingStatus.BEREGNET,
        ) {
            endreTilStatus(it)
        }

    override fun tilFattetVedtak(): ManueltOpphoer {
        if (!erFyltUt) {
            logger.info(("Behandling ($id) må være fylt ut for å settes til fattet vedtak"))
            throw TilstandException.IkkeFyltUt
        }

        return hvisTilstandEr(listOf(BehandlingStatus.BEREGNET, BehandlingStatus.RETURNERT), BehandlingStatus.FATTET_VEDTAK) {
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
        hvisTilstandEr(listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.TIL_SAMORDNING), BehandlingStatus.SAMORDNET) {
            endreTilStatus(it)
        }

    override fun tilIverksatt() =
        hvisTilstandEr(listOf(BehandlingStatus.ATTESTERT, BehandlingStatus.SAMORDNET), BehandlingStatus.IVERKSATT) {
            endreTilStatus(it)
        }

    private fun endreTilStatus(status: BehandlingStatus) =
        this.copy(
            status = status,
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
        )
}
