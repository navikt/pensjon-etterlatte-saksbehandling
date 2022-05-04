package no.nav.etterlatte.behandling

import java.util.*

class BehandlingFactory(private val behandlinger: BehandlingDao) {
    fun hent(id: UUID):BehandlingAggregat = BehandlingAggregat(id, behandlinger)
    fun opprett(sakId: Long):BehandlingAggregat = BehandlingAggregat.opprett(sakId, behandlinger)
}