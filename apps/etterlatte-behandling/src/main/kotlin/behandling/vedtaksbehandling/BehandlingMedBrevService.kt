package no.nav.etterlatte.behandling.vedtaksbehandling

import java.util.UUID

class BehandlingMedBrevService(
    private val behandlingMedBrevDao: BehandlingMedBrevDao,
) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean = behandlingMedBrevDao.erBehandlingRedigerbar(behandlingId)

    fun hentBehandlingMedBrev(behandlingId: UUID): BehandlingMedBrev = behandlingMedBrevDao.hentBehandlingMedBrev(behandlingId)
}
