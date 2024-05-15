package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import java.time.YearMonth
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
    val sendeBrev: Boolean,
    val opphoerFraOgMed: YearMonth? = null,
)

fun DetaljertBehandling.virkningstidspunkt() =
    requireNotNull(virkningstidspunkt) {
        "Mangler virkningstidspunkt for behandling=$id"
    }
