package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.girOpphoer
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.*

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregnBarnepensjonService: BeregnBarnepensjonService,
    private val beregnOmstillingsstoenadService: BeregnOmstillingsstoenadService,
    private val beregningsGrunnlagService: BeregningsGrunnlagService
) {
    private val logger = LoggerFactory.getLogger(BeregningService::class.java)

    fun hentBeregning(behandlingId: UUID): Beregning? {
        logger.info("Henter beregning for behandlingId=$behandlingId")
        return beregningRepository.hent(behandlingId)
    }

    fun hentBeregningNonnull(behandlingId: UUID): Beregning {
        return hentBeregning(behandlingId) ?: throw Exception("Mangler beregning for behandlingId=$behandlingId")
    }

    suspend fun opprettBeregning(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): Beregning {
        logger.info("Oppretter beregning for behandlingId=$behandlingId")
        val kanBeregneYtelse = behandlingKlient.beregn(behandlingId, brukerTokenInfo, commit = false)
        if (kanBeregneYtelse) {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

            val beregning = when (behandling.sakType) {
                SakType.BARNEPENSJON -> beregnBarnepensjonService.beregn(behandling, brukerTokenInfo)
                SakType.OMSTILLINGSSTOENAD -> beregnOmstillingsstoenadService.beregn(behandling, brukerTokenInfo)
            }

            val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)
            behandlingKlient.beregn(behandlingId, brukerTokenInfo, commit = true)
            return lagretBeregning
        } else {
            throw IllegalStateException("Kan ikke beregne behandlingId=$behandlingId, behandling er i feil tilstand")
        }
    }

    suspend fun opprettForOpphoer(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo) {
        val kanBeregneYtelse = behandlingKlient.beregn(behandlingId, brukerTokenInfo, commit = false)
        if (kanBeregneYtelse) {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
            if (behandling.revurderingsaarsak.girOpphoer()) {
                kopierBeregningsgrunnlagOgOpprettBeregning(behandling, brukerTokenInfo, behandlingId)
            }
        }
    }

    private suspend fun kopierBeregningsgrunnlagOgOpprettBeregning(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID
    ) {
        val sistIverksatte = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo)
        val grunnlagDenneBehandlinga =
            beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(behandlingId, brukerTokenInfo)
        if (grunnlagDenneBehandlinga == null) {
            logger.info("Kopierer beregningsgrunnlag og oppretter beregning for $behandlingId")
            beregningsGrunnlagService.dupliserBeregningsGrunnlag(behandlingId, sistIverksatte.id)
            opprettBeregning(behandlingId, brukerTokenInfo)
        }
    }
}