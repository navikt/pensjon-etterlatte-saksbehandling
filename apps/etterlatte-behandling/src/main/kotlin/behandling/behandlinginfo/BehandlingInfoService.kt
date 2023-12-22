package no.nav.etterlatte.behandling.behandlinginfo

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

class BehandlingInfoService(
    private val behandlingInfoDao: BehandlingInfoDao,
    private val behandlingService: BehandlingService,
    private val behandlingsstatusService: BehandlingStatusService,
) {
    fun lagreBrevutfall(
        behandlingId: UUID,
        brevutfall: Brevutfall,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brevutfall {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw GenerellIkkeFunnetException()

        sjekkBehandlingKanEndres(behandling)
        sjekkAldersgruppeSattVedBarnepensjon(behandling, brevutfall)

        val utfall = behandlingInfoDao.lagreBrevutfall(brevutfall)
        oppdaterBehandlingStatus(behandling, brukerTokenInfo)
        return utfall
    }

    fun hentBrevutfall(behandlingId: UUID): Brevutfall? {
        return behandlingInfoDao.hentBrevutfall(behandlingId)
    }

    fun lagreEtterbetaling(
        behandlingId: UUID,
        etterbetaling: Etterbetaling?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Etterbetaling? {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw GenerellIkkeFunnetException()

        sjekkBehandlingKanEndres(behandling)

        if (etterbetaling == null) {
            hentEtterbetaling(behandlingId)?.let {
                behandlingInfoDao.slettEtterbetaling(behandlingId)
            }
            return null
        }

        sjekkEtterbetalingFoerVirkningstidspunkt(behandling, etterbetaling)
        val etterbetaling = behandlingInfoDao.lagreEtterbetaling(etterbetaling)
        oppdaterBehandlingStatus(behandling, brukerTokenInfo)
        return etterbetaling
    }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? {
        return behandlingInfoDao.hentEtterbetaling(behandlingId)
    }

    private fun sjekkBehandlingKanEndres(behandling: Behandling) {
        if (!behandling.status.kanEndres()) throw BrevutfallException.BehandlingKanIkkeEndres(behandling.id, behandling.status)
    }

    private fun sjekkEtterbetalingFoerVirkningstidspunkt(
        behandling: Behandling,
        etterbetaling: Etterbetaling,
    ) {
        val virkningstidspunkt =
            behandling.virkningstidspunkt?.dato
                ?: throw BrevutfallException.VirkningstidspunktIkkeSatt(behandling.id)

        if (etterbetaling.fom < virkningstidspunkt) {
            throw EtterbetalingException.EtterbetalingFraDatoErFoerVirk(etterbetaling.fom, virkningstidspunkt)
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

    private fun oppdaterBehandlingStatus(
        behandling: Behandling,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        when (behandling.sak.sakType) {
            SakType.BARNEPENSJON -> behandlingsstatusService.settBeregnet(behandling.id, brukerTokenInfo)
            SakType.OMSTILLINGSSTOENAD -> behandlingsstatusService.settAvkortet(behandling.id, brukerTokenInfo)
        }
    }
}
