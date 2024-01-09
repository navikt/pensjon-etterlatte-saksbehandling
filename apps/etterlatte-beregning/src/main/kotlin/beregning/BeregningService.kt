package no.nav.etterlatte.beregning

import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregnBarnepensjonService: BeregnBarnepensjonService,
    private val beregnOmstillingsstoenadService: BeregnOmstillingsstoenadService,
    private val beregnOverstyrBeregningService: BeregnOverstyrBeregningService,
) {
    private val logger = LoggerFactory.getLogger(BeregningService::class.java)

    suspend fun hentBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = hentBeregning(behandlingId).berikMedOverstyrBeregning(brukerTokenInfo)

    fun hentBeregningNonnull(behandlingId: UUID): Beregning {
        return hentBeregning(behandlingId) ?: throw Exception("Mangler beregning for behandlingId=$behandlingId")
    }

    suspend fun opprettBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Beregning {
        logger.info("Oppretter beregning for behandlingId=$behandlingId")
        val kanBeregneYtelse = behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, commit = false)
        if (kanBeregneYtelse) {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

            val overstyrBeregning = hentOverstyrBeregning(behandlingId, brukerTokenInfo)

            val beregning =
                if (overstyrBeregning != null) {
                    beregnOverstyrBeregningService.beregn(behandling, overstyrBeregning, brukerTokenInfo)
                } else {
                    when (behandling.sakType) {
                        SakType.BARNEPENSJON -> beregnBarnepensjonService.beregn(behandling, brukerTokenInfo)
                        SakType.OMSTILLINGSSTOENAD ->
                            beregnOmstillingsstoenadService.beregn(
                                behandling,
                                brukerTokenInfo,
                            )
                    }
                }

            val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)
            behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, commit = true)
            return lagretBeregning.berikMedOverstyrBeregning(brukerTokenInfo) ?: lagretBeregning
        } else {
            throw IllegalStateException("Kan ikke beregne behandlingId=$behandlingId, behandling er i feil tilstand")
        }
    }

    suspend fun hentOverstyrBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregning? {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        return beregningRepository.hentOverstyrBeregning(behandling.sak)
    }

    suspend fun opprettOverstyrBeregning(
        behandlingId: UUID,
        overstyrBeregning: OverstyrBeregningDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregning? {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        return hentOverstyrBeregning(behandlingId, brukerTokenInfo).takeIf { it != null }
            ?: beregningRepository.opprettOverstyrBeregning(
                OverstyrBeregning(
                    behandling.sak,
                    overstyrBeregning.beskrivelse,
                    Tidspunkt.now(),
                ),
            )
    }

    private fun hentBeregning(behandlingId: UUID): Beregning? {
        logger.info("Henter beregning for behandlingId=$behandlingId")

        return beregningRepository.hent(behandlingId)
    }

    private suspend fun Beregning?.berikMedOverstyrBeregning(brukerTokenInfo: BrukerTokenInfo) =
        this?.copy(overstyrBeregning = hentOverstyrBeregning(behandlingId, brukerTokenInfo))
}

class TrygdetidMangler(behandlingId: UUID) : UgyldigForespoerselException(
    code = "TRYGDETID_MANGLER",
    detail = "Trygdetid ikke satt for behandling $behandlingId",
)

class BeregningsgrunnlagMangler(behandlingId: UUID) : UgyldigForespoerselException(
    code = "BEREGNINGSGRUNNLAG_MANGLER",
    detail = "Behandling med id: $behandlingId mangler beregningsgrunnlag oms",
)

class AnvendtGrunnbeloepIkkeFunnet : UgyldigForespoerselException(
    code = "ANVENDT_GRUNNBELOEP_IKKE_FUNNET",
    detail = "Anvendt grunnbeløp ikke funnet for perioden",
)

class AnvendtTrygdetidIkkeFunnet : UgyldigForespoerselException(
    code = "ANVENDT_TRYGDETID_IKKE_FUNNET",
    detail = "Anvendt trygdetid ikke funnet for perioden",
)
