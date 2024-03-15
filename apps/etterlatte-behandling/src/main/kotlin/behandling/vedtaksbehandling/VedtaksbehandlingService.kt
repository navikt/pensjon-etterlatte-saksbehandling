package no.nav.etterlatte.behandling.vedtaksbehandling

import java.util.UUID

class VedtaksbehandlingService(
    private val vedtaksbehandlingDao: VedtaksbehandlingDao,
    private val fiksVedtaktstilstandDao: FiksVedtaktstilstandDao,
) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean {
        return vedtaksbehandlingDao.erBehandlingRedigerbar(behandlingId)
    }

    fun hentAktuelle() = fiksVedtaktstilstandDao.finnAktuelleBehandlinger()
}
