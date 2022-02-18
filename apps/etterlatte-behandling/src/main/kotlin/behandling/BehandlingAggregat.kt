package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.*
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class BehandlingAggregat(
    id: UUID,
    private val behandlinger: BehandlingDao,
    private val opplysninger: OpplysningDao,
    private val vilkaarKlient: VilkaarKlient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BehandlingAggregat::class.java)

        fun opprett(
            sak: Long,
            behandlinger: BehandlingDao,
            opplysninger: OpplysningDao,
            vilkaarKlient: VilkaarKlient
        ): BehandlingAggregat {
            return Behandling(UUID.randomUUID(), sak, emptyList(), null, null)
                .also {
                    behandlinger.opprett(it)
                    logger.info("Opprettet behandling ${it.id} i sak ${it.sak}")
                }
                .let { BehandlingAggregat(it.id, behandlinger, opplysninger, vilkaarKlient) }
        }
    }

    private var lagretBehandling = requireNotNull(behandlinger.hent(id))
    private var lagredeOpplysninger = opplysninger.finnOpplysningerIBehandling(id)

    fun leggTilGrunnlagListe(nyeOpplysninger: List<Behandlingsopplysning<ObjectNode>>) {
        if (nyeOpplysninger.isEmpty()) return
        for (opplysning in nyeOpplysninger) {
            leggTilGrunnlagUtenVilkårsprøving(opplysning.opplysning, opplysning.opplysningType, opplysning.kilde)
        }
        vilkårsprøv()
    }

    fun leggTilGrunnlagUtenVilkårsprøving(data: ObjectNode, type: String, kilde: Behandlingsopplysning.Kilde?): UUID {
        val behandlingsopplysning = Behandlingsopplysning(
            UUID.randomUUID(),
            kildeFraRequestContekst(kilde),
            type,
            objectMapper.createObjectNode(),
            data
        )
        opplysninger.nyOpplysning(behandlingsopplysning)
        opplysninger.leggOpplysningTilBehandling(lagretBehandling.id, behandlingsopplysning.id)
        lagredeOpplysninger += behandlingsopplysning
        logger.info("La til opplysning $type i behandling ${lagretBehandling.id}")
        return behandlingsopplysning.id
    }

    fun vilkårsprøv() {
        vilkaarKlient
            .vurderVilkaar(lagredeOpplysninger).also {
                lagretBehandling = lagretBehandling.copy(
                    vilkårsprøving = it
                )
                behandlinger.lagreVilkarsproving(lagretBehandling)
            }

    }

    private fun kildeFraRequestContekst(oppgittKilde: Behandlingsopplysning.Kilde?): Behandlingsopplysning.Kilde {
        return if (Kontekst.get().AppUser.kanSetteKilde() && oppgittKilde != null) oppgittKilde else when (Kontekst.get().AppUser) {
            is Saksbehandler -> if (oppgittKilde == null) Behandlingsopplysning.Saksbehandler(Kontekst.get().AppUser.name()) else throw IllegalArgumentException()
            is Kunde -> if (oppgittKilde == null) Behandlingsopplysning.Privatperson(
                Kontekst.get().AppUser.name(),
                Instant.now()
            ) else throw IllegalArgumentException()
            else -> throw IllegalArgumentException()
        }
    }

    fun serialiserbarUtgave() = lagretBehandling.copy(grunnlag = lagredeOpplysninger)
}