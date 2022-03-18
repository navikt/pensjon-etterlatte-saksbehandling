package no.nav.etterlatte.behandling

import java.util.*

class BehandlingFactory(private val behandlinger: BehandlingDao,
                        private val opplysninger: OpplysningDao,
                        private val vilkaarKlient: VilkaarKlient,) {

    fun hent(id: UUID):BehandlingAggregat = BehandlingAggregat(id, behandlinger, opplysninger, vilkaarKlient)
    fun opprett(sakId: Long):BehandlingAggregat = BehandlingAggregat.opprett(sakId, behandlinger, opplysninger, vilkaarKlient)
}