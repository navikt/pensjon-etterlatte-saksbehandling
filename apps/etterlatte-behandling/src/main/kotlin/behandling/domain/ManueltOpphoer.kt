package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime
import java.util.*

data class ManueltOpphoer(
    override val id: UUID,
    override val sak: Sak,
    override val behandlingOpprettet: LocalDateTime,
    override val sistEndret: LocalDateTime,
    override val status: BehandlingStatus,
    override val persongalleri: Persongalleri,
    override val virkningstidspunkt: Virkningstidspunkt?,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?,
    override val prosesstype: Prosesstype = Prosesstype.MANUELL
) : Behandling() {
    override val type: BehandlingType = BehandlingType.MANUELT_OPPHOER
    override val kilde = Vedtaksloesning.GJENNY

    constructor(
        sak: Sak,
        persongalleri: Persongalleri,
        opphoerAarsaker: List<ManueltOpphoerAarsak>,
        fritekstAarsak: String?,
        virkningstidspunkt: Virkningstidspunkt?
    ) : this(
        id = UUID.randomUUID(),
        sak = sak,
        behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
        sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
        status = BehandlingStatus.OPPRETTET,
        persongalleri = persongalleri,
        opphoerAarsaker = opphoerAarsaker,
        fritekstAarsak = fritekstAarsak,
        virkningstidspunkt = virkningstidspunkt
    )

    private val erFyltUt: Boolean
        get() {
            return (virkningstidspunkt != null)
        }

    override val kommerBarnetTilgode: KommerBarnetTilgode?
        get() = null

    override fun tilBeregnet(): ManueltOpphoer = hvisTilstandEr(
        listOf(
            BehandlingStatus.OPPRETTET,
            BehandlingStatus.BEREGNET,
            BehandlingStatus.RETURNERT
        )
    ) {
        endreTilStatus(BehandlingStatus.BEREGNET)
    }

    override fun tilFattetVedtak(): ManueltOpphoer {
        if (!erFyltUt) {
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

    private fun endreTilStatus(status: BehandlingStatus) = this.copy(
        status = status,
        sistEndret = Tidspunkt.now().toLocalDatetimeUTC()
    )
}