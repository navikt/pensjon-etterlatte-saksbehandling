package no.nav.etterlatte.behandling

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import java.util.UUID

class BehandlingHenter(
    private val behandlingDao: BehandlingDao,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentBehandling(behandlingId: UUID): Behandling? {
        return inTransaction {
            hentBehandlingForId(behandlingId)
        }
    }

    internal fun hentBehandlingForId(id: UUID) =
        behandlingDao.hentBehandling(id)?.let { behandling ->
            listOf(behandling).filterForEnheter().firstOrNull()
        }

    fun hentBehandlingerISak(sakId: Long): List<Behandling> {
        return inTransaction {
            hentBehandlingerForSakId(sakId)
        }
    }

    internal fun hentBehandlingerForSakId(sakId: Long) = behandlingDao.alleBehandlingerISak(sakId).filterForEnheter()

    private fun List<Behandling>.filterForEnheter() =
        this.filterBehandlingerForEnheter(
            featureToggleService = featureToggleService,
            user = Kontekst.get().AppUser,
        )
}
