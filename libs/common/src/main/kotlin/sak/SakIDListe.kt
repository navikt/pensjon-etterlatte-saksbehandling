package no.nav.etterlatte.libs.common.sak

import java.util.*

data class SakIDListe(val ider: List<BehandlingOgSak>) {
    fun behandlingerForSak(id: Long): List<UUID> = ider.filter { it.sakId == id }.map { it.behandlingId }
}

data class BehandlingOgSak(val behandlingId: UUID, val sakId: Long)