package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.util.UUID

data class DetaljertBehandling(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val soeker: String, // statistikk og vedtak
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val revurderingsaarsak: RevurderingAarsak? = null,
    val revurderingInfo: RevurderingInfo?,
    val prosesstype: Prosesstype, // statistikk og trygdetid
) {
    fun kanVedta(type: VedtakType): Boolean {
        if (revurderingsaarsak.girOpphoer() && type != VedtakType.OPPHOER) {
            return false
        }
        return true
    }
}

fun RevurderingAarsak?.girOpphoer() = this != null && utfall.girOpphoer
