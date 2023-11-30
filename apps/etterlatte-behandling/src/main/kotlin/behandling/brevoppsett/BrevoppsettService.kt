package no.nav.etterlatte.behandling.brevoppsett

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import java.util.UUID

class BrevoppsettService(
    private val brevoppsettDao: BrevoppsettDao,
    private val behandlingService: BehandlingService,
) {
    fun lagreBrevoppsett(brevoppsett: Brevoppsett) {
        val behandling =
            behandlingService.hentBehandling(brevoppsett.behandlingId)
                ?: throw GenerellIkkeFunnetException()

        if (behandlingKanEndres(brevoppsett.behandlingId)) {
            // TODO gjør validering av feks virk vs etterbetaling-datoer
            brevoppsettDao.lagre(brevoppsett)
        }
        throw behandlingKanIkkeEndres(behandling)
    }

    fun hentBrevoppsett(behandlingId: UUID): Brevoppsett? {
        return brevoppsettDao.hent(behandlingId)
    }

    private fun behandlingKanEndres(behandlingId: UUID): Boolean {
        val behandling = behandlingService.hentBehandling(behandlingId) ?: throw GenerellIkkeFunnetException()
        return behandling.status.kanEndres()
    }

    private fun behandlingKanIkkeEndres(behandling: Behandling) =
        IkkeTillattException(
            code = "KAN_IKKE_ENDRES",
            detail = "Behandling ${behandling.id} har status ${behandling.status} og kan ikke endres",
        )
}
