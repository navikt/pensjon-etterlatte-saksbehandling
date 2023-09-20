package no.nav.etterlatte.ytelseMedGrunnlag

import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagPeriodisertDto
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

class YtelseMedGrunnlagService(
    private val beregningRepository: BeregningRepository,
    private val avkortingRepository: AvkortingRepository,
    private val behandlingKlient: BehandlingKlient,
) {
    suspend fun hentYtelseMedGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): YtelseMedGrunnlagDto? {
        val avkortingUtenLoependeYtelse = avkortingRepository.hentAvkorting(behandlingId) ?: return null
        val virkningstidspunkt =
            behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                .virkningstidspunkt?.dato ?: throw Exception("Behandling $behandlingId mangler virkningstidspunkt")
        val avkorting = avkortingUtenLoependeYtelse.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt)

        val beregning = beregningRepository.hent(behandlingId)

        val avkortinger =
            avkorting.avkortetYtelseFraVirkningstidspunkt.map { avkortetYtelse ->
                beregning ?: throw Exception("Mangler beregning for behandling $behandlingId")
                val beregningIPeriode =
                    beregning.beregningsperioder
                        .filter { it.datoFOM <= avkortetYtelse.periode.fom }
                        .maxBy { it.datoFOM }

                val avkortingsgrunnlagIPeriode =
                    avkorting.aarsoppgjoer.inntektsavkorting
                        .filter { it.grunnlag.periode.fom <= avkortetYtelse.periode.fom }
                        .maxBy { it.grunnlag.periode.fom }

                YtelseMedGrunnlagPeriodisertDto(
                    periode = avkortetYtelse.periode,
                    ytelseEtterAvkorting = avkortetYtelse.ytelseEtterAvkorting,
                    avkortingsbeloep = avkortetYtelse.avkortingsbeloep,
                    ytelseFoerAvkorting = beregningIPeriode.utbetaltBeloep,
                    trygdetid = beregningIPeriode.trygdetid,
                    aarsinntekt = avkortingsgrunnlagIPeriode.grunnlag.aarsinntekt,
                    fratrekkInnAar = avkortingsgrunnlagIPeriode.grunnlag.fratrekkInnAar,
                    grunnbelop = beregningIPeriode.grunnbelop,
                    grunnbelopMnd = beregningIPeriode.grunnbelopMnd,
                )
            }

        return YtelseMedGrunnlagDto(
            perioder = avkortinger,
        )
    }
}
