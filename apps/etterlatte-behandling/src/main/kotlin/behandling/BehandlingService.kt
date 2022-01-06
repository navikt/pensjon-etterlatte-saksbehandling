package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.*

interface BehandlingService {
    fun hentBehandling(behandling: UUID): Behandling?
    fun hentBehandlinger(): List<Behandling>
    fun hentBehandlingerISak(sakid: Long): List<Behandling>
    fun startBehandling(sak: Long): Behandling
    fun leggTilGrunnlag(behandling: UUID, data: ObjectNode, type: String, kilde: Opplysning.Kilde? = null): UUID
    fun vilkårsprøv(behandling: UUID)
    fun beregn(behandling: UUID, beregning: Beregning)
    fun ferdigstill(behandling: UUID)
}

class RealBehandlingService(private val behandlinger: BehandlingDao, private val opplysninger: OpplysningDao, private val vilkaarKlient: VilkaarKlient) : BehandlingService {
    override fun hentBehandling(behandling: UUID): Behandling {
        return BehandlingAggregat(behandling, behandlinger, opplysninger, vilkaarKlient).serialiserbarUtgave()
    }

    override fun hentBehandlinger(): List<Behandling> {
        return behandlinger.alle()
    }

    override fun hentBehandlingerISak(sakid: Long): List<Behandling> {
        return behandlinger.alleISak(sakid)
    }

    override fun startBehandling(sak: Long): Behandling {
        return BehandlingAggregat.opprett(sak, behandlinger, opplysninger, vilkaarKlient).serialiserbarUtgave() }

    override fun leggTilGrunnlag(behandling: UUID, data: ObjectNode, type: String, kilde: Opplysning.Kilde?): UUID {
        return BehandlingAggregat(behandling, behandlinger, opplysninger, vilkaarKlient).leggTilGrunnlag(data, type, kilde)
    }

    override fun vilkårsprøv(behandling: UUID) {
        BehandlingAggregat(behandling, behandlinger, opplysninger, vilkaarKlient).vilkårsprøv()
    }

    override fun beregn(behandling: UUID, beregning: Beregning) {
        val lagretBehandling = requireNotNull(behandlinger.hent(behandling))
        require(!lagretBehandling.fastsatt)
        requireNotNull(lagretBehandling.vilkårsprøving)
        behandlinger.lagreBeregning(lagretBehandling.copy(beregning = beregning))
    }

    override fun ferdigstill(behandling: UUID) {
        TODO("Not yet implemented")
    }

}
