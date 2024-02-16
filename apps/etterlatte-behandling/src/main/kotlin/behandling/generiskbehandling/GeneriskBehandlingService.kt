package no.nav.etterlatte.behandling.generiskbehandling

import java.util.UUID

class GeneriskBehandlingService(private val generiskBehandlingDao: GeneriskBehandlingDao) {
    fun erBehandlingRedigerbar(behandlingId: UUID) {
        generiskBehandlingDao.erBehandlingRedigerbar(behandlingId)
    }
}
