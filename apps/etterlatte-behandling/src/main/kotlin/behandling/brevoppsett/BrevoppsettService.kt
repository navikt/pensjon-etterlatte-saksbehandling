package no.nav.etterlatte.behandling.brevoppsett

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import java.util.UUID

class BrevoppsettService(
    private val brevoppsettDao: BrevoppsettDao,
    private val behandlingService: BehandlingService,
) {
    fun lagreBrevoppsett(brevoppsett: Brevoppsett): Brevoppsett {
        val behandling =
            behandlingService.hentBehandling(brevoppsett.behandlingId)
                ?: throw GenerellIkkeFunnetException()

        sjekkBehandlingKanEndres(behandling)
        sjekkEtterbetalingFoerVirkningstidspunkt(behandling, brevoppsett)
        sjekkAldersgruppeSattVedBarnepensjon(behandling, brevoppsett)

        return brevoppsettDao.lagre(brevoppsett)
    }

    fun hentBrevoppsett(behandlingId: UUID): Brevoppsett? {
        return brevoppsettDao.hent(behandlingId)
    }

    private fun sjekkBehandlingKanEndres(behandling: Behandling) {
        if (!behandling.status.kanEndres()) throw BrevoppsettException.BehandlingKanIkkeEndres(behandling)
    }

    private fun sjekkEtterbetalingFoerVirkningstidspunkt(
        behandling: Behandling,
        brevoppsett: Brevoppsett,
    ) {
        val virkningstidspunkt =
            behandling.virkningstidspunkt?.dato
                ?: throw BrevoppsettException.VirkningstidspunktIkkeSatt(behandling)

        if (brevoppsett.etterbetaling != null && brevoppsett.etterbetaling.fom < virkningstidspunkt) {
            throw BrevoppsettException.EtterbetalingFraDatoErFoerVirk(brevoppsett.etterbetaling.fom, virkningstidspunkt)
        }
    }

    private fun sjekkAldersgruppeSattVedBarnepensjon(
        behandling: Behandling,
        brevoppsett: Brevoppsett,
    ) {
        if (behandling.sak.sakType == SakType.BARNEPENSJON && brevoppsett.aldersgruppe == null) {
            throw BrevoppsettException.AldergruppeIkkeSatt()
        }
    }
}
