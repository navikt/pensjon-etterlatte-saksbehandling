package no.nav.etterlatte.behandling.behandlinginfo

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class BehandlingInfoService(
    private val behandlingInfoDao: BehandlingInfoDao,
    private val behandlingService: BehandlingService,
    private val behandlingsstatusService: BehandlingStatusService,
) {
    fun lagreBrevutfallOgEtterbetaling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        opphoer: Boolean,
        brevutfall: Brevutfall,
        etterbetaling: Etterbetaling?,
    ): Pair<Brevutfall, Etterbetaling?> {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw GenerellIkkeFunnetException()

        sjekkBehandlingKanEndres(behandling, opphoer)

        val lagretBrevutfall = lagreBrevutfall(behandling, opphoer, brevutfall)
        val lagretEtterbetaling = lagreEtterbetaling(behandling, etterbetaling)

        oppdaterBehandlingStatus(behandling, opphoer, brukerTokenInfo)

        return Pair(lagretBrevutfall, lagretEtterbetaling)
    }

    private fun lagreBrevutfall(
        behandling: Behandling,
        opphoer: Boolean,
        brevutfall: Brevutfall,
    ): Brevutfall {
        sjekkAldersgruppeSattVedBarnepensjon(behandling, brevutfall)
        sjekkLavEllerIngenInntektSattVedOmstillingsstoenad(behandling, opphoer, brevutfall)
        sjekkFeilutbetalingErSatt(behandling, brevutfall)
        return behandlingInfoDao.lagreBrevutfall(brevutfall)
    }

    fun hentBrevutfall(behandlingId: UUID): Brevutfall? {
        return behandlingInfoDao.hentBrevutfall(behandlingId)
    }

    fun lagreEtterbetaling(
        behandling: Behandling,
        etterbetaling: Etterbetaling?,
    ): Etterbetaling? {
        if (etterbetaling == null) {
            hentEtterbetaling(behandling.id)?.let {
                behandlingInfoDao.slettEtterbetaling(behandling.id)
            }
            return null
        }

        sjekkEtterbetalingFoerVirkningstidspunkt(behandling, etterbetaling)
        return behandlingInfoDao.lagreEtterbetaling(etterbetaling)
    }

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? {
        return behandlingInfoDao.hentEtterbetaling(behandlingId)
    }

    private fun sjekkBehandlingKanEndres(
        behandling: Behandling,
        opphoer: Boolean,
    ) {
        val kanEndres =
            when (behandling.sak.sakType) {
                SakType.BARNEPENSJON -> {
                    if (opphoer) {
                        behandling.status in
                            listOf(
                                BehandlingStatus.VILKAARSVURDERT,
                                BehandlingStatus.RETURNERT,
                            )
                    } else {
                        behandling.status in
                            listOf(
                                BehandlingStatus.BEREGNET,
                                BehandlingStatus.RETURNERT,
                            )
                    }
                }

                SakType.OMSTILLINGSSTOENAD ->
                    if (opphoer) {
                        behandling.status in
                            listOf(
                                BehandlingStatus.VILKAARSVURDERT,
                                BehandlingStatus.RETURNERT,
                            )
                    } else {
                        behandling.status in
                            listOf(
                                BehandlingStatus.AVKORTET,
                                BehandlingStatus.RETURNERT,
                            )
                    }
            }
        if (!kanEndres) {
            throw BrevutfallException.BehandlingKanIkkeEndres(
                behandling.id,
                behandling.status,
            )
        }
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

    private fun sjekkLavEllerIngenInntektSattVedOmstillingsstoenad(
        behandling: Behandling,
        opphoer: Boolean,
        brevutfall: Brevutfall,
    ) {
        if (behandling.sak.sakType == SakType.OMSTILLINGSSTOENAD &&
            !opphoer &&
            brevutfall.lavEllerIngenInntekt == null
        ) {
            throw BrevutfallException.LavEllerIngenInntektIkkeSatt()
        }
    }

    private fun sjekkFeilutbetalingErSatt(
        behandling: Behandling,
        brevutfall: Brevutfall,
    ) {
        if (behandling.type == BehandlingType.REVURDERING &&
            brevutfall.feilutbetaling == null
        ) {
            throw BrevutfallException.FeilutbetalingIkkeSatt()
        }
    }

    private fun oppdaterBehandlingStatus(
        behandling: Behandling,
        opphoer: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (opphoer) {
            behandlingsstatusService.settVilkaarsvurdert(behandling.id, brukerTokenInfo, false)
        } else {
            when (behandling.sak.sakType) {
                SakType.BARNEPENSJON -> behandlingsstatusService.settBeregnet(behandling.id, brukerTokenInfo, false)
                SakType.OMSTILLINGSSTOENAD -> behandlingsstatusService.settAvkortet(behandling.id, brukerTokenInfo, false)
            }
        }
    }
}
