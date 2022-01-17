package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Kunde
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.behandling.Opplysning
import java.time.Instant
import java.util.*

class BehandlingAggregat(id: UUID, private val behandlinger: BehandlingDao, private val opplysninger: OpplysningDao, private val vilkaarKlient: VilkaarKlient) {
    companion object{
        fun opprett(sak: Long, behandlinger: BehandlingDao, opplysninger: OpplysningDao, vilkaarKlient: VilkaarKlient): BehandlingAggregat{
            return Behandling(UUID.randomUUID(), sak, emptyList(), null, null)
                .also { behandlinger.opprett(it) }
                .let { BehandlingAggregat(it.id, behandlinger, opplysninger, vilkaarKlient) }
        }
    }

    private var lagretBehandling = requireNotNull( behandlinger.hent(id))
    private var lagredeOpplysninger = opplysninger.finnOpplysningerIBehandling(id)

    fun leggTilGrunnlag(data: ObjectNode, type: String, kilde: Opplysning.Kilde?): UUID {
        require(lagretBehandling.vilkårsprøving == null)

        val opplysning = Opplysning(UUID.randomUUID(), kildeFraRequestContekst(kilde), type, objectMapper.createObjectNode(), data)
        opplysninger.nyOpplysning(opplysning)
        opplysninger.leggOpplysningTilBehandling(lagretBehandling.id, opplysning.id)
        lagredeOpplysninger += opplysning
        return opplysning.id
    }

    fun vilkårsprøv() {
        vilkaarKlient
            .vurderVilkaar("barnepensjon:brukerungnok", lagredeOpplysninger).also {
                lagretBehandling = lagretBehandling.copy(vilkårsprøving = Vilkårsprøving(resultat = it, opplysninger = lagredeOpplysninger.map { it.id.toString() }, ansvarlig = Kontekst.get().AppUser.name()))
                behandlinger.lagreVilkarsproving(lagretBehandling)
            }

    }

    private fun kildeFraRequestContekst(oppgittKilde: Opplysning.Kilde?): Opplysning.Kilde {
        return when (Kontekst.get().AppUser) {
            is Saksbehandler -> if(oppgittKilde == null) Opplysning.Saksbehandler(Kontekst.get().AppUser.name()) else throw IllegalArgumentException()
            is Kunde -> if(oppgittKilde == null) Opplysning.Privatperson(Kontekst.get().AppUser.name(), Instant.now()) else throw IllegalArgumentException()
            is Self -> requireNotNull(oppgittKilde)
            else -> throw IllegalArgumentException()
        }
    }

    fun serialiserbarUtgave() = lagretBehandling.copy(grunnlag = lagredeOpplysninger)
}