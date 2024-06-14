package no.nav.etterlatte.ytelseMedGrunnlag

import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagPeriodisertDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
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
        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo).virkningstidspunkt()
        val avkorting = avkortingUtenLoependeYtelse.medYtelseFraOgMedVirkningstidspunkt(virkningstidspunkt.dato)

        val beregning = beregningRepository.hent(behandlingId) ?: throw BeregningFinnesIkkeException(behandlingId)

        val avkortinger =
            avkorting.avkortetYtelseFraVirkningstidspunkt.map { avkortetYtelse ->
                val beregningIPeriode =
                    beregning.beregningsperioder
                        .filter { it.datoFOM <= avkortetYtelse.periode.fom }
                        .maxBy { it.datoFOM }

                // TODO erstatt single..
                val aarsoppgjoer = avkorting.aarsoppgjoer.single()
                val avkortingsgrunnlagIPeriode =
                    aarsoppgjoer.inntektsavkorting
                        .filter { it.grunnlag.periode.fom <= avkortetYtelse.periode.fom }
                        .maxBy { it.grunnlag.periode.fom }

                val grunnlag = avkortingsgrunnlagIPeriode.grunnlag
                val aarsinntekt = grunnlag.aarsinntekt + grunnlag.inntektUtland
                val fratrekkInnAar = grunnlag.fratrekkInnAar + grunnlag.fratrekkInnAarUtland

                YtelseMedGrunnlagPeriodisertDto(
                    periode = avkortetYtelse.periode,
                    ytelseEtterAvkorting = avkortetYtelse.ytelseEtterAvkorting,
                    restanse = avkortetYtelse.restanse?.fordeltRestanse ?: 0,
                    avkortingsbeloep = avkortetYtelse.avkortingsbeloep,
                    ytelseFoerAvkorting = beregningIPeriode.utbetaltBeloep,
                    trygdetid = beregningIPeriode.trygdetid,
                    aarsinntekt = aarsinntekt,
                    fratrekkInnAar = fratrekkInnAar,
                    relevanteMaanederInnAar = aarsoppgjoer.forventaInnvilgaMaaneder,
                    grunnbelop = beregningIPeriode.grunnbelop,
                    grunnbelopMnd = beregningIPeriode.grunnbelopMnd,
                    beregningsMetode = beregningIPeriode.beregningsMetode,
                )
            }

        return YtelseMedGrunnlagDto(
            perioder = avkortinger,
        )
    }
}

class BeregningFinnesIkkeException(
    behandlingId: UUID,
) : IkkeFunnetException(
        code = "BEREGNING_IKKE_FUNNET",
        detail = "Mangler beregning for behandling $behandlingId",
    )
