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
        val behandling =
            inTransaction {
                requireNotNull(behandlingService.hentBehandling(behandlingId))
            }

        if (!behandling.status.kanEndres()) {
            throw IllegalStateException(
                "Kan ikke opprette sjekkliste for behandling ${behandling.id} med status ${behandling.status}",
            )
        } else if (hentSjekkliste(behandlingId) != null) {
            throw IllegalStateException("Det finnes allerede en sjekkliste for behandling ${behandling.id}")
        }

        val items =
            when (behandling.sak.sakType) {
                SakType.BARNEPENSJON -> defaultSjekklisteItemsBP
                SakType.OMSTILLINGSSTOENAD -> defaultSjekklisteItemsOMS
            }

        inTransaction {
            dao.opprettSjekkliste(behandling.id, items)
        }

        return requireNotNull(hentSjekkliste(behandling.id))
    }

    fun oppdaterSjekkliste(
        behandlingId: UUID,
        oppdaterSjekkliste: OppdatertSjekkliste,
    ): Sjekkliste {
        return inTransaction {
            val behandling = requireNotNull(behandlingService.hentBehandling(behandlingId))
            if (!behandling.status.kanEndres()) {
                throw IllegalStateException("Kan ikke oppdatere sjekkliste for behandling ${behandling.id} med status ${behandling.status}")
            }

            dao.oppdaterSjekkliste(behandlingId, oppdaterSjekkliste)
            dao.hentSjekkliste(behandlingId)!!
        }
    }

    fun oppdaterSjekklisteItem(
        behandlingId: UUID,
        itemId: Long,
        oppdatering: OppdaterSjekklisteItem,
    ): SjekklisteItem {
        return inTransaction {
            val behandling = requireNotNull(behandlingService.hentBehandling(behandlingId))
            if (!behandling.status.kanEndres()) {
                throw IllegalStateException(
                    "Kan ikke oppdatere sjekklisteelement for behandling ${behandling.id} med status ${behandling.status}",
                )
            }

            dao.oppdaterSjekklisteItem(itemId, oppdatering)
            dao.hentSjekklisteItem(itemId)
        }
    }
}
