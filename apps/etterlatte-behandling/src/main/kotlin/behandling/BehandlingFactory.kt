package no.nav.etterlatte.behandling

import java.util.*

class BehandlingFactory(private val behandlinger: BehandlingDao,
                        private val opplysninger: OpplysningDao) {

    fun hent(id: UUID):BehandlingAggregat = BehandlingAggregat(id, behandlinger, opplysninger)
    fun opprett(sakId: Long):BehandlingAggregat = BehandlingAggregat.opprett(sakId, behandlinger, opplysninger)
}