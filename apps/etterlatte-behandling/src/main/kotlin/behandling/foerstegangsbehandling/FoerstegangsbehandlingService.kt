package no.nav.etterlatte.behandling.foerstegangsbehandling

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
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
    fun lagreVirkningstidspunkt(
        behandlingId: UUID,
        dato: YearMonth,
        ident: String,
        begrunnelse: String
    ): Virkningstidspunkt

    fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode)
    fun settOpprettet(behandlingId: UUID, dryRun: Boolean = true)
    fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean = true, utfall: VilkaarsvurderingUtfall?)
    fun settBeregnet(behandlingId: UUID, dryRun: Boolean = true)
    fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean = true)
    fun settAttestert(behandlingId: UUID, dryRun: Boolean = true)
    fun settReturnert(behandlingId: UUID, dryRun: Boolean = true)
    fun settIverksatt(behandlingId: UUID, dryRun: Boolean = true)
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
                .lagreGyldighetproeving(gyldighetsproeving)
        }
    }

    override fun lagreVirkningstidspunkt(
        behandlingId: UUID,
        dato: YearMonth,
        ident: String,
        begrunnelse: String
    ): Virkningstidspunkt {
        return inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandlingId)
                .lagreVirkningstidspunkt(dato, ident, begrunnelse)
        }
    }

    override fun lagreKommerBarnetTilgode(behandlingId: UUID, kommerBarnetTilgode: KommerBarnetTilgode) {
        return inTransaction {
            foerstegangsbehandlingFactory.hentFoerstegangsbehandling(behandlingId)
                .lagreKommerBarnetTilgode(kommerBarnetTilgode)
        }
    }

    override fun settOpprettet(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilOpprettet().lagreEndring(dryRun)
    }

    override fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean, utfall: VilkaarsvurderingUtfall?) {
        val behandling = hentFoerstegangsbehandling(behandlingId).tilVilkaarsvurdert(utfall)

        if (!dryRun) {
            inTransaction {
                behandlinger.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)
                behandlinger.lagreVilkaarstatus(behandling.id, behandling.vilkaarUtfall)
            }
        }
    }

    override fun settBeregnet(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilBeregnet().lagreEndring(dryRun)
    }

    override fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilFattetVedtak().lagreEndring(dryRun)
    }

    override fun settAttestert(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilAttestert().lagreEndring(dryRun)
    }

    override fun settReturnert(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilReturnert().lagreEndring(dryRun)
    }

    override fun settIverksatt(behandlingId: UUID, dryRun: Boolean) {
        hentFoerstegangsbehandling(behandlingId).tilIverksatt().lagreEndring(dryRun)
    }

    private fun Foerstegangsbehandling.lagreEndring(dryRun: Boolean) {
        if (dryRun) return

        lagreNyBehandlingStatus(this)
    }

    private fun lagreNyBehandlingStatus(behandling: Foerstegangsbehandling) {
        inTransaction {
            behandling.let {
                behandlinger.lagreStatus(it.id, it.status, it.sistEndret)
            }
        }
    }
}