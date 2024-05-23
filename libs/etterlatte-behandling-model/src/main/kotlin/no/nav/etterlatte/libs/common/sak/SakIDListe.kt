package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.behandling.SakType
import java.util.UUID

data class SakIDListe(val ider: List<BehandlingOgSak>) {
    fun behandlingerForSak(id: Long): List<UUID> = ider.filter { it.sakId == id }.map { it.behandlingId }
}

data class BehandlingOgSak(val behandlingId: UUID, val sakId: Long)

data class HentSakerRequest(
    val sakIder: List<Long>,
    val sakType: SakType?,
)
