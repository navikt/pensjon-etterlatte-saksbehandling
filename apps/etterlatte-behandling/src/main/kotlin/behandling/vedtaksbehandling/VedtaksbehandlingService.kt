package no.nav.etterlatte.behandling.vedtaksbehandling

import java.util.UUID

class VedtaksbehandlingService(
    private val vedtaksbehandlingDao: VedtaksbehandlingDao,
) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean = vedtaksbehandlingDao.erBehandlingRedigerbar(behandlingId)

    fun hentVedtaksbehandling(behandlingId: UUID): Vedtaksbehandling = vedtaksbehandlingDao.hentVedtaksbehandling(behandlingId)
}
