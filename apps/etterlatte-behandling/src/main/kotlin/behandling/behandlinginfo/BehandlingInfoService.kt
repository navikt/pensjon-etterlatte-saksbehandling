package no.nav.etterlatte.behandling.behandlinginfo

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.utland.SluttbehandlingBehandlinginfo
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandlingInfoService(
    private val behandlingInfoDao: BehandlingInfoDao,
    private val behandlingService: BehandlingService,
    private val behandlingsstatusService: BehandlingStatusService,
) {
    private val logger = LoggerFactory.getLogger(BehandlingInfoService::class.java)

    fun lagreBrevutfallOgEtterbetaling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        opphoer: Boolean,
        brevutfallDto: BrevutfallDto,
        etterbetaling: Etterbetaling?,
    ): Pair<BrevutfallDto, Etterbetaling?> {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw GenerellIkkeFunnetException()

        if (behandling.revurderingsaarsak() != Revurderingaarsak.AARLIG_INNTEKTSJUSTERING &&
            !(
                behandling.revurderingsaarsak() == Revurderingaarsak.INNTEKTSENDRING &&
                    behandling.prosesstype == Prosesstype.AUTOMATISK
            )
        ) {
            sjekkBehandlingKanEndres(behandling, opphoer)
        }

        val lagretBrevutfall = lagreBrevutfall(behandling, brevutfallDto)
        val lagretEtterbetaling = lagreEtterbetaling(behandling, etterbetaling)

        oppdaterBehandlingStatus(behandling, opphoer, brukerTokenInfo)

        return Pair(lagretBrevutfall, lagretEtterbetaling)
    }

    fun lagreErOmgjoeringSluttbehandlingUtland(behandling: Behandling) = behandlingInfoDao.lagreErOmgjoeringSluttbehandling(behandling.id)

    fun lagreSluttbehandling(
        behandlingId: UUID,
        sluttbehandling: SluttbehandlingBehandlinginfo,
    ) {
        behandlingInfoDao.lagreSluttbehandling(behandlingId, sluttbehandling)
    }

    fun hentSluttbehandling(behandlingId: UUID): SluttbehandlingBehandlinginfo? = behandlingInfoDao.hentSluttbehandling(behandlingId)

    private fun lagreBrevutfall(
        behandling: Behandling,
        brevutfallDto: BrevutfallDto,
    ): BrevutfallDto {
        sjekkAldersgruppeSattVedBarnepensjon(behandling, brevutfallDto)
        sjekkFeilutbetalingErSatt(behandling, brevutfallDto)
        return behandlingInfoDao.lagreBrevutfall(brevutfallDto)
    }

    fun hentBrevutfall(behandlingId: UUID): BrevutfallDto? = behandlingInfoDao.hentBrevutfall(behandlingId)

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

    fun hentEtterbetaling(behandlingId: UUID): Etterbetaling? = behandlingInfoDao.hentEtterbetaling(behandlingId)

    private fun sjekkBehandlingKanEndres(
        behandling: Behandling,
        opphoer: Boolean,
    ) {
        // TODO: sjekke her burde sjekke rekkefølge som i frontend... har man en senere eller lik status burde vi her si OK ikke spørre om spesifikk status...
        val kanEndres =
            when (behandling.sak.sakType) {
                SakType.BARNEPENSJON -> {
                    if (opphoer) {
                        behandling.status in
                            listOf(
                                BehandlingStatus.VILKAARSVURDERT,
                                BehandlingStatus.TRYGDETID_OPPDATERT,
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
        brevutfallDto: BrevutfallDto,
    ) {
        if (behandling.sak.sakType == SakType.BARNEPENSJON && brevutfallDto.aldersgruppe == null) {
            throw BrevutfallException.AldergruppeIkkeSatt()
        }
    }

    private fun sjekkFeilutbetalingErSatt(
        behandling: Behandling,
        brevutfallDto: BrevutfallDto,
    ) {
        if (behandling.type == BehandlingType.REVURDERING &&
            brevutfallDto.feilutbetaling == null
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
                SakType.OMSTILLINGSSTOENAD ->
                    behandlingsstatusService.settAvkortet(
                        behandling.id,
                        brukerTokenInfo,
                        false,
                    )
            }
        }
    }
}
