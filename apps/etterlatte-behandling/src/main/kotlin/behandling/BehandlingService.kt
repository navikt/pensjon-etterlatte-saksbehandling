package no.nav.etterlatte.behandling

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingService {
    fun hentBehandling(behandling: UUID): Behandling?
    fun hentBehandlinger(): List<Behandling>
    fun hentBehandlingerISak(sakid: Long): List<Behandling>
    fun startBehandling(sak: Long, persongalleri: Persongalleri, mottattDato: String): Behandling
    fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat)
    fun slettBehandlingerISak(sak: Long)
    fun avbrytBehandling(behandling: UUID): Behandling
    fun grunnlagISakEndret(sak: Long)
}

class RealBehandlingService(
    private val behandlinger: BehandlingDao,
    private val behandlingFactory: BehandlingFactory,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>
) : BehandlingService {
    private val logger = LoggerFactory.getLogger(RealBehandlingService::class.java)

    override fun hentBehandling(behandling: UUID): Behandling {
        return inTransaction { behandlingFactory.hent(behandling).serialiserbarUtgave() }
    }

    override fun hentBehandlinger(): List<Behandling> {
        return inTransaction { behandlinger.alleBehandlinger() }
    }

    override fun hentBehandlingerISak(sakid: Long): List<Behandling> {
        return inTransaction {
            behandlinger.alleBehandingerISak(sakid).map {
                behandlingFactory.hent(it.id).serialiserbarUtgave()
            }
        }
    }

    override fun startBehandling(sak: Long, persongalleri: Persongalleri, mottattDato: String): Behandling {
        logger.info("Starter en behandling")
        return inTransaction {
            behandlingFactory.opprett(sak)
               .also { behandling ->
                    behandling.leggTilPersongalleriOgDato(persongalleri, mottattDato)
                }

        }.also {
            runBlocking {
                behandlingHendelser.send(it.lagretBehandling.id to BehandlingHendelseType.OPPRETTET)
            }
        }.serialiserbarUtgave()
    }


    override fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat) {
        inTransaction { behandlingFactory.hent(behandling).lagreGyldighetprøving(gyldighetsproeving)}
    }

    override fun slettBehandlingerISak(sak: Long) {
        inTransaction {
            println("Sletter alle behandlinger i sak: $sak")
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

    override fun grunnlagISakEndret(sak: Long) {
        inTransaction {
            behandlinger.alleBehandingerISak(sak)
        }.also {
            runBlocking {
                it.forEach{
                    behandlingHendelser.send(it.id to BehandlingHendelseType.GRUNNLAGENDRET)
                }
            }
        }
    }

}
