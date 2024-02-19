package no.nav.etterlatte.behandling.generiskbehandling

import java.util.UUID

class GeneriskBehandlingService(private val generiskBehandlingDao: GeneriskBehandlingDao) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean {
        return generiskBehandlingDao.erBehandlingRedigerbar(behandlingId)
    }
}
