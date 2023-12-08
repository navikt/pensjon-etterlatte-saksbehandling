package no.nav.etterlatte.behandling.brevutfall

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import java.util.UUID

class BrevutfallService(
    private val brevutfallDao: BrevutfallDao,
    private val behandlingService: BehandlingService,
) {
    fun lagreBrevutfall(brevutfall: Brevutfall): Brevutfall {
        val behandling =
            behandlingService.hentBehandling(brevutfall.behandlingId)
                ?: throw GenerellIkkeFunnetException()

        sjekkBehandlingKanEndres(behandling)
        sjekkEtterbetalingFoerVirkningstidspunkt(behandling, brevutfall)
        sjekkAldersgruppeSattVedBarnepensjon(behandling, brevutfall)

        return brevutfallDao.lagre(brevutfall)
    }

    fun hentBrevutfall(behandlingId: UUID): Brevutfall? {
        return brevutfallDao.hent(behandlingId)
    }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? {
        return brevutfallDao.hent(behandlingId)?.etterbetaling
    }

    private fun sjekkBehandlingKanEndres(behandling: Behandling) {
        if (!behandling.status.kanEndres()) throw BrevutfallException.BehandlingKanIkkeEndres(behandling)
    }

    private fun sjekkEtterbetalingFoerVirkningstidspunkt(
        behandling: Behandling,
        brevutfall: Brevutfall,
    ) {
        val virkningstidspunkt =
            behandling.virkningstidspunkt?.dato
                ?: throw BrevutfallException.VirkningstidspunktIkkeSatt(behandling)

        if (brevutfall.etterbetaling != null && brevutfall.etterbetaling.fom < virkningstidspunkt) {
            throw BrevutfallException.EtterbetalingFraDatoErFoerVirk(brevutfall.etterbetaling.fom, virkningstidspunkt)
        }
    }

    private fun sjekkAldersgruppeSattVedBarnepensjon(
        behandling: Behandling,
        brevutfall: Brevutfall,
    ) {
        if (behandling.sak.sakType == SakType.BARNEPENSJON && brevutfall.aldersgruppe == null) {
            throw BrevutfallException.AldergruppeIkkeSatt()
        }
    }
}
