package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.behandling.SakType
import java.util.UUID

data class SakIDListe(
    val tilbakestileBehandlinger: List<BehandlingOgSak>,
    val aapneBehandlinger: List<BehandlingOgSak>,
) {
    fun tilbakestilteForSak(id: Long): List<UUID> = tilbakestileBehandlinger.filter { it.sakId == id }.map { it.behandlingId }

    fun aapneBehandlingerForSak(id: Long): List<UUID> = aapneBehandlinger.filter { it.sakId == id }.map { it.behandlingId }
}

data class BehandlingOgSak(
    val behandlingId: UUID,
    val sakId: SakId,
)

data class HentSakerRequest(
    val spesifikkeSaker: List<Long>,
    val ekskluderteSaker: List<Long>,
    val sakType: SakType?,
)
