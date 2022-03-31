package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.Beregning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingService {
    fun hentBehandling(behandling: UUID): Behandling?
    fun hentBehandlinger(): List<Behandling>
    fun hentBehandlingerISak(sakid: Long): List<Behandling>
    fun startBehandling(sak: Long, nyeOpplysninger: List<Behandlingsopplysning<ObjectNode>>): Behandling
    fun leggTilGrunnlagFraRegister(behandling: UUID, opplysninger: List<Behandlingsopplysning<ObjectNode>>)
    fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat)
    fun lagreVilkårsprøving(behandling: UUID, vilkaarsproeving: VilkaarResultat)
    fun beregn(behandling: UUID, beregning: Beregning)
    fun ferdigstill(behandling: UUID)
    fun slettBehandlingerISak(sak: Long)
    fun avbrytBehandling(behandling: UUID): Behandling
}

class RealBehandlingService(
    private val behandlinger: BehandlingDao,
    private val opplysninger: OpplysningDao,
    private val behandlingFactory: BehandlingFactory,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>
) : BehandlingService {
    private val logger = LoggerFactory.getLogger(RealBehandlingService::class.java)

    override fun hentBehandling(behandling: UUID): Behandling {
        return inTransaction { behandlingFactory.hent(behandling).serialiserbarUtgave() }
    }

    override fun hentBehandlinger(): List<Behandling> {
        return inTransaction { behandlinger.alle() }
    }

    override fun hentBehandlingerISak(sakid: Long): List<Behandling> {
        return inTransaction {
            behandlinger.alleISak(sakid).map {
                behandlingFactory.hent(it.id).serialiserbarUtgave()
            }
        }
    }

    override fun startBehandling(sak: Long, nyeOpplysninger: List<Behandlingsopplysning<ObjectNode>>): Behandling {
        logger.info("Starter en behandling")
        return inTransaction {
            behandlingFactory.opprett(sak)
                .also { behandling ->
                    behandling.leggTilGrunnlagListe(nyeOpplysninger)
                }

        }.also {
            runBlocking {
                behandlingHendelser.send(it.lagretBehandling.id to BehandlingHendelseType.OPPRETTET)
            }
        }.serialiserbarUtgave()
    }

    override fun leggTilGrunnlagFraRegister(
        behandlingsId: UUID,
        opplysninger: List<Behandlingsopplysning<ObjectNode>>
    ) {
        inTransaction { behandlingFactory.hent(behandlingsId).leggTilGrunnlagListe(
            opplysninger
        )}.also {
            runBlocking { behandlingHendelser.send(behandlingsId to BehandlingHendelseType.GRUNNLAGENDRET) }
        }
    }

    override fun lagreVilkårsprøving(behandling: UUID, vilkaarsproeving: VilkaarResultat) {
        inTransaction { behandlingFactory.hent(behandling).lagreVilkårsprøving(vilkaarsproeving)}
    }

    override fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat) {
        inTransaction { behandlingFactory.hent(behandling).lagreGyldighetprøving(gyldighetsproeving)}
    }

    override fun beregn(behandling: UUID, beregning: Beregning) {
        inTransaction { val lagretBehandling = requireNotNull(behandlinger.hent(behandling))
        require(!lagretBehandling.fastsatt)
        requireNotNull(lagretBehandling.vilkårsprøving)
        behandlinger.lagreBeregning(lagretBehandling.copy(beregning = beregning))}
    }

    override fun ferdigstill(behandling: UUID) {
        TODO("Not yet implemented")
    }

    override fun slettBehandlingerISak(sak: Long) {
        inTransaction {
            println("Sletter alle behandlinger i sak: $sak")
            opplysninger.slettOpplysningerISak(sak)
            behandlinger.slettBehandlingerISak(sak)
        }
    }

    override fun avbrytBehandling(behandling: UUID): Behandling {
        return inTransaction { behandlingFactory.hent(behandling).avbrytBehandling()}.also {
            runBlocking {
                behandlingHendelser.send(behandling to BehandlingHendelseType.AVBRUTT)
            }
        }
    }

}
