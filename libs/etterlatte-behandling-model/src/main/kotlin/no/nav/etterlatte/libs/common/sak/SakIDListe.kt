package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.YearMonth
import java.util.UUID

data class SakIDListe(
    val tilbakestileBehandlinger: List<BehandlingOgSak>,
    val aapneBehandlinger: List<BehandlingOgSak>,
) {
    fun tilbakestilteForSak(id: SakId): List<UUID> = tilbakestileBehandlinger.filter { it.sakId == id }.map { it.behandlingId }

    fun aapneBehandlingerForSak(id: SakId): List<UUID> = aapneBehandlinger.filter { it.sakId == id }.map { it.behandlingId }
}

data class BehandlingOgSak(
    val behandlingId: UUID,
    val sakId: SakId,
)

data class HentSakerRequest(
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
    val sakType: SakType?,
    val loependeFom: YearMonth?,
)
