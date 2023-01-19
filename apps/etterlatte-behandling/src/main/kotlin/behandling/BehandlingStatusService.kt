package no.nav.etterlatte.behandling

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.util.*

interface BehandlingStatusService {
    fun settOpprettet(behandlingId: UUID, dryRun: Boolean = true)
    fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean = true, utfall: VilkaarsvurderingUtfall?)
    fun settBeregnet(behandlingId: UUID, dryRun: Boolean = true)
    fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean = true)
    fun settAttestert(behandlingId: UUID, dryRun: Boolean = true)
    fun settReturnert(behandlingId: UUID, dryRun: Boolean = true)
    fun settIverksatt(behandlingId: UUID, dryRun: Boolean = true)
}

class BehandlingStatusServiceImpl constructor(private val behandlingDao: BehandlingDao) : BehandlingStatusService {
    override fun settOpprettet(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilOpprettet().lagreEndring(dryRun)
    }

    override fun settVilkaarsvurdert(behandlingId: UUID, dryRun: Boolean, utfall: VilkaarsvurderingUtfall?) {
        val behandling = hentBehandling(behandlingId).tilVilkaarsvurdert(utfall)

        if (!dryRun) {
            inTransaction {
                behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)
                behandlingDao.lagreVilkaarstatus(behandling.id, behandling.vilkaarUtfall)
            }
        }
    }

    override fun settBeregnet(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilBeregnet().lagreEndring(dryRun)
    }

    override fun settFattetVedtak(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilFattetVedtak().lagreEndring(dryRun)
    }

    override fun settAttestert(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilAttestert().lagreEndring(dryRun)
    }

    override fun settReturnert(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilReturnert().lagreEndring(dryRun)
    }

    override fun settIverksatt(behandlingId: UUID, dryRun: Boolean) {
        hentBehandling(behandlingId).tilIverksatt().lagreEndring(dryRun)
    }

    private fun Behandling.lagreEndring(dryRun: Boolean) {
        if (dryRun) return

        lagreNyBehandlingStatus(this)
    }

    private fun lagreNyBehandlingStatus(behandling: Behandling) {
        inTransaction {
            behandling.let {
                behandlingDao.lagreStatus(it.id, it.status, it.sistEndret)
            }
        }
    }

    private fun hentBehandling(behandlingId: UUID): Behandling = inTransaction {
        behandlingDao.hentBehandling(behandlingId) ?: throw NotFoundException(
            "Fant ikke behandling med id=$behandlingId"
        )
    }
}