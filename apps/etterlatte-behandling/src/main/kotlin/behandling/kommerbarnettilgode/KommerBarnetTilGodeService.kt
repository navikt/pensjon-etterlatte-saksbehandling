package behandling.kommerbarnettilgode

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import java.util.*

class KommerBarnetTilGodeService(
    private val behandlingService: BehandlingService,
    private val kommerBarnetTilGodeDao: KommerBarnetTilGodeDao,
    private val behandlingDao: BehandlingDao
) {

    fun lagreKommerBarnetTilgode(kommerBarnetTilgode: KommerBarnetTilgode) {
        return inTransaction {
            kommerBarnetTilgode.behandlingId?.let {
                behandlingService.hentBehandling(it)
                    ?.tilOpprettet()
                    ?.also { kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(kommerBarnetTilgode) }
                    ?.also { behandling -> behandlingDao.lagreStatus(behandling) }
            }
        }
    }

    fun hentKommerBarnetTilGode(behandlingId: UUID) = kommerBarnetTilGodeDao.hentKommerBarnetTilGode(behandlingId)
}