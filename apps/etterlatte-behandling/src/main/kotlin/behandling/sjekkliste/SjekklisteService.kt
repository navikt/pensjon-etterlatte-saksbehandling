package no.nav.etterlatte.behandling.sjekkliste

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import java.util.UUID

class SjekklisteService(
    private val dao: SjekklisteDao,
    private val behandlingService: BehandlingService,
) {
    fun hentSjekkliste(id: UUID): Sjekkliste? {
        return inTransaction {
            dao.hentSjekkliste(id)
        }
    }

    fun opprettSjekkliste(behandlingId: UUID): Sjekkliste {
        val behandling = requireNotNull(behandlingService.hentBehandling(behandlingId))

        if (!behandling.status.kanEndres()) {
            throw IllegalStateException("Kan ikke opprette sjekkliste for behandling ${behandling.id} med status ${behandling.status}")
        } else if (hentSjekkliste(behandling.id) != null) {
            throw IllegalStateException("Det finnes allerede en sjekkliste for behandling ${behandling.id}")
        }

        val items =
            when (behandling.sak.sakType) {
                SakType.BARNEPENSJON -> defaultSjekklisteItemsBP
                SakType.OMSTILLINGSSTOENAD -> defaultSjekklisteItemsOMS
            }

        return inTransaction {
            dao.opprettSjekkliste(behandling.id, items)
            requireNotNull(hentSjekkliste(behandling.id))
        }
    }

    fun oppdaterSjekkliste(
        behandlingId: UUID,
        oppdaterSjekkliste: OppdaterSjekkliste,
    ) {
        val behandling = requireNotNull(behandlingService.hentBehandling(behandlingId))
        if (!behandling.status.kanEndres()) {
            throw IllegalStateException("Kan ikke oppdatere sjekkliste for behandling ${behandling.id} med status ${behandling.status}")
        }
    }

    fun oppdaterSjekklisteItem(
        behandlingId: UUID,
        itemId: Long,
        oppdatering: OppdaterSjekklisteItem,
    ): SjekklisteItem {
        val behandling = requireNotNull(behandlingService.hentBehandling(behandlingId))
        if (!behandling.status.kanEndres()) {
            throw IllegalStateException("Kan ikke oppdatere sjekkliste for behandling ${behandling.id} med status ${behandling.status}")
        }

        return inTransaction {
            dao.oppdaterSjekklisteItem(itemId, oppdatering)
            dao.hentSjekklisteItem(itemId)
        }
    }
}
