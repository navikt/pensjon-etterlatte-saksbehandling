package no.nav.etterlatte.behandling.foerstegangsbehandling

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.*

interface FoerstegangsbehandlingService {
    fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling?
    fun startFoerstegangsbehandling(
        sak: Long,
        persongalleri: Persongalleri,
        mottattDato: String
    ): Foerstegangsbehandling

    fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat)
    fun lagreVirkningstidspunkt(behandlingId: UUID, dato: YearMonth, ident: String): Virkningstidspunkt
    fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode)
    fun settVilkaarsvurdert(behandlingId: UUID)
    fun settBeregnet(behandlingId: UUID)
    fun settFattetVedtak(behandlingId: UUID)
    fun settAttestert(behandlingId: UUID)
    fun settReturnert(behandlingId: UUID)
    fun settIverksatt(behandlingId: UUID)
}

class RealFoerstegangsbehandlingService(
    private val behandlinger: BehandlingDao,
    private val foerstegangsbehandlingFactory: FoerstegangsbehandlingFactory,
    private val behandlingHendelser: SendChannel<Pair<UUID, BehandlingHendelseType>>
) : FoerstegangsbehandlingService {
    private val logger = LoggerFactory.getLogger(RealFoerstegangsbehandlingService::class.java)

    override fun hentFoerstegangsbehandling(behandling: UUID): Foerstegangsbehandling {
        return inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling).serialiserbarUtgave()
        }
    }

    override fun startFoerstegangsbehandling(
        sak: Long,
        persongalleri: Persongalleri,
        mottattDato: String
    ): Foerstegangsbehandling {
        logger.info("Starter en behandling")
        return inTransaction {
            foerstegangsbehandlingFactory.opprettFoerstegangsbehandling(
                sak,
                mottattDato,
                persongalleri
            )
        }.also {
            runBlocking {
                behandlingHendelser.send(it.lagretBehandling.id to BehandlingHendelseType.OPPRETTET)
            }
        }.serialiserbarUtgave()
    }

    override fun lagreGyldighetsprøving(behandling: UUID, gyldighetsproeving: GyldighetsResultat) {
        inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandling)
                .lagreGyldighetprøving(gyldighetsproeving)
        }
    }

    override fun lagreVirkningstidspunkt(behandlingId: UUID, dato: YearMonth, ident: String): Virkningstidspunkt {
        return inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandlingId).lagreVirkningstidspunkt(dato, ident)
        }
    }

    override fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode) {
        return inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandlingId)
                .lagreKommerBarnetTilgode(kommerBarnetTilgode)
        }
    }

    override fun settVilkaarsvurdert(behandlingId: UUID) {
        lagreNyBehandlingStatus(hentFoerstegangsbehandling(behandlingId).tilVilkaarsvurdert())
    }

    override fun settBeregnet(behandlingId: UUID) {
        lagreNyBehandlingStatus(hentFoerstegangsbehandling(behandlingId).tilBeregnet())
    }

    override fun settFattetVedtak(behandlingId: UUID) {
        lagreNyBehandlingStatus(hentFoerstegangsbehandling(behandlingId).tilFattetVedtak())
    }

    override fun settAttestert(behandlingId: UUID) {
        lagreNyBehandlingStatus(hentFoerstegangsbehandling(behandlingId).tilAttestert())
    }

    override fun settReturnert(behandlingId: UUID) {
        lagreNyBehandlingStatus(hentFoerstegangsbehandling(behandlingId).tilReturnert())
    }

    override fun settIverksatt(behandlingId: UUID) {
        lagreNyBehandlingStatus(hentFoerstegangsbehandling(behandlingId).tilIverksatt())
    }

    private fun lagreNyBehandlingStatus(behandling: Foerstegangsbehandling) {
        inTransaction {
            behandling.let {
                behandlinger.lagreStatus(it.id, it.status, it.sistEndret)
            }
        }
    }
}