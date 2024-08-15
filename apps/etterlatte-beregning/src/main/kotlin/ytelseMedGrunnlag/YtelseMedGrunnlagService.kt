package no.nav.etterlatte.ytelseMedGrunnlag

import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagPeriodisertDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.periode.Periode
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
        val avkorting = avkortingUtenLoependeYtelse.toDto(virkningstidspunkt.dato)

        val beregning = beregningRepository.hent(behandlingId) ?: throw BeregningFinnesIkkeException(behandlingId)

        val avkortinger =
            avkorting.avkortetYtelse.map { avkortetYtelse ->
                val beregningIPeriode =
                    beregning.beregningsperioder
                        .filter { it.datoFOM <= avkortetYtelse.fom }
                        .maxBy { it.datoFOM }
                val grunnlag =
                    avkorting.avkortingGrunnlag
                        .filter { it.fom <= avkortetYtelse.fom }
                        .maxBy { it.fom }
                val aarsinntekt = grunnlag.aarsinntekt + grunnlag.inntektUtland
                val fratrekkInnAar = grunnlag.fratrekkInnAar + grunnlag.fratrekkInnAarUtland

                YtelseMedGrunnlagPeriodisertDto(
                    periode = Periode(avkortetYtelse.fom, avkortetYtelse.tom),
                    ytelseEtterAvkorting = avkortetYtelse.ytelseEtterAvkorting,
                    restanse = avkortetYtelse.restanse,
                    avkortingsbeloep = avkortetYtelse.avkortingsbeloep,
                    ytelseFoerAvkorting = beregningIPeriode.utbetaltBeloep,
                    trygdetid = beregningIPeriode.trygdetid,
                    aarsinntekt = aarsinntekt,
                    fratrekkInnAar = fratrekkInnAar,
                    relevanteMaanederInnAar = grunnlag.relevanteMaanederInnAar,
                    grunnbelop = beregningIPeriode.grunnbelop,
                    grunnbelopMnd = beregningIPeriode.grunnbelopMnd,
                    beregningsMetode = beregningIPeriode.beregningsMetode,
                    sanksjon = avkortetYtelse.sanksjon,
                    institusjonsopphold = beregningIPeriode.institusjonsopphold,
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
