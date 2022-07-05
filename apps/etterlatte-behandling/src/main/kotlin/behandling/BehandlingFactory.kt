package no.nav.etterlatte.behandling

import java.util.*

class BehandlingFactory(private val behandlinger: BehandlingDao, private val hendelser: HendelseDao) {
    fun hent(id: UUID):BehandlingAggregat = BehandlingAggregat(id, behandlinger, hendelser)
    fun opprett(sakId: Long):BehandlingAggregat = BehandlingAggregat.opprett(sakId, behandlinger, hendelser)
}