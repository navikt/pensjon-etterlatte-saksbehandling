package no.nav.etterlatte.behandling


import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Kunde
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.Self
import java.time.Instant
import java.util.*

interface BehandlingService {
    fun hentBehandling(behandling: UUID): Behandling?
    fun hentBehandlinger(): List<Behandling>
    fun startBehandling(sak: String): Behandling
    fun leggTilGrunnlag(behandling: UUID, data: ObjectNode, type: String, kilde: Opplysning.Kilde? = null): UUID
    fun vilkårsprøv(behandling: UUID, vilkårsprøving: Vilkårsprøving)
    fun beregn(behandling: UUID, beregning: Beregning)
    fun ferdigstill(behandling: UUID)
}

class RealBehandlingService(private val behandlinger: BehandlingDao, private val opplysninger: OpplysningDao) : BehandlingService {
    override fun hentBehandling(behandling: UUID): Behandling? {
        return behandlinger.hent(behandling)?.copy(grunnlag = opplysninger.finnOpplysningerIBehandling(behandling))
    }

    override fun hentBehandlinger(): List<Behandling> {
        return behandlinger.alle()
    }

    override fun startBehandling(sak: String): Behandling {
        return Behandling(UUID.randomUUID(), sak, emptyList(), null, null)
            .also { behandlinger.opprett(it) }
            .let { requireNotNull(behandlinger.hent(it.id)) }
    }

    override fun leggTilGrunnlag(behandling: UUID, data: ObjectNode, type: String, kilde: Opplysning.Kilde?): UUID {
        val lagretBehandling = behandlinger.hent(behandling)
            ?: throw IllegalStateException("forsøker å lagre vilkårsprøving på behandling som ikke eksisterer")
        require(lagretBehandling.vilkårsprøving == null)

        val opplysning = Opplysning(UUID.randomUUID(), kildeFraRequestContekst(kilde), type, objectMapper.createObjectNode(), data)
        opplysninger.nyOpplysning(opplysning)
        opplysninger.leggOpplysningTilBehandling(lagretBehandling.id, opplysning.id)
        return opplysning.id
    }

    override fun vilkårsprøv(behandling: UUID, vilkårsprøving: Vilkårsprøving) {
        val lagretBehandling = behandlinger.hent(behandling)?: throw IllegalStateException("forsøker å lagre vilkårsprøving på behandling som ikke eksisterer")
        require(lagretBehandling.vilkårsprøving == null)
        behandlinger.lagreVilkarsproving(lagretBehandling.copy(vilkårsprøving = vilkårsprøving.copy(ansvarlig = Kontekst.get().AppUser.name())))

    }

    private fun kildeFraRequestContekst(oppgittKilde: Opplysning.Kilde?): Opplysning.Kilde {
        return when (Kontekst.get().AppUser) {
            is Saksbehandler -> if(oppgittKilde == null) Opplysning.Saksbehandler(Kontekst.get().AppUser.name()) else throw IllegalArgumentException()
            is Kunde -> if(oppgittKilde == null) Opplysning.Privatperson(Kontekst.get().AppUser.name(), Instant.now()) else throw IllegalArgumentException()
            is Self -> requireNotNull(oppgittKilde)
            else -> throw IllegalArgumentException()
        }
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