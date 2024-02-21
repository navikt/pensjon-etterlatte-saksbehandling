package no.nav.etterlatte.behandling.vedtaksbehandling

import java.util.UUID

class VedtaksbehandlingService(private val vedtaksbehandlingDao: VedtaksbehandlingDao) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean {
        return vedtaksbehandlingDao.erBehandlingRedigerbar(behandlingId)
    }
}
