package no.nav.etterlatte.behandling.kommerbarnettilgode

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import java.util.UUID

class KommerBarnetTilGodeService(
    private val kommerBarnetTilGodeDao: KommerBarnetTilGodeDao,
    private val behandlingDao: BehandlingDao,
) {
    fun lagreKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode) {
        kommerBarnetTilgode.behandlingId?.let {
            behandlingDao
                .hentBehandling(it)
                ?.tilOpprettet()
                ?.also { kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(kommerBarnetTilgode) }
                ?.also { behandling -> behandlingDao.lagreStatus(behandling) }
        }
    }

    fun hentKommerBarnetTilGode(behandlingId: UUID) = kommerBarnetTilGodeDao.hentKommerBarnetTilGode(behandlingId)
}
