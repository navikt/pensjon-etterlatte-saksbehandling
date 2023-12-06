package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.util.UUID

data class DetaljertBehandling(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val soeker: String,
    val status: BehandlingStatus,
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val utlandstilknytning: Utlandstilknytning?,
    val revurderingsaarsak: Revurderingaarsak? = null,
    val revurderingInfo: RevurderingInfo?,
    val prosesstype: Prosesstype,
    val kilde: Vedtaksloesning,
) {
    fun kanVedta(type: VedtakType): Boolean {
        return !(revurderingsaarsak.girOpphoer() && type != VedtakType.OPPHOER)
    }
}

fun Revurderingaarsak?.girOpphoer() = this != null && utfall.girOpphoer

fun DetaljertBehandling.virkningstidspunkt() =
    requireNotNull(virkningstidspunkt) {
        "Mangler virkningstidspunkt for behandling=$id"
    }
