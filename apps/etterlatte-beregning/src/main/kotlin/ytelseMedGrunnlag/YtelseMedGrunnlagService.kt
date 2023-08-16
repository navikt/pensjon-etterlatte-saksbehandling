package no.nav.etterlatte.ytelseMedGrunnlag

import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagPeriodisertDto
import java.util.*

class YtelseMedGrunnlagService(
    private val beregningRepository: BeregningRepository,
    private val avkortingRepository: AvkortingRepository
) {

    fun hentYtelseMedGrunnlag(behandlingId: UUID): YtelseMedGrunnlagDto? {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)
        val beregning = beregningRepository.hent(behandlingId)
        val avkortinger = avkorting?.avkortetYtelse?.map { avkortetYtelse ->

            beregning ?: throw Exception("Mangler beregning for behandling $behandlingId")
            val beregningIPeriode = beregning.beregningsperioder
                .filter { it.datoFOM <= avkortetYtelse.periode.fom }
                .maxBy { it.datoFOM }

            val avkortingsgrunnlagIPeriode = avkorting.avkortingGrunnlag
                .filter { it.periode.fom <= avkortetYtelse.periode.fom }
                .maxBy { it.periode.fom }

            YtelseMedGrunnlagPeriodisertDto(
                periode = avkortetYtelse.periode,
                ytelseEtterAvkorting = avkortetYtelse.ytelseEtterAvkorting,
                avkortingsbeloep = avkortetYtelse.avkortingsbeloep,
                ytelseFoerAvkorting = beregningIPeriode.utbetaltBeloep,
                trygdetid = beregningIPeriode.trygdetid,
                aarsinntekt = avkortingsgrunnlagIPeriode.aarsinntekt,
                fratrekkInnAar = avkortingsgrunnlagIPeriode.fratrekkInnAar,
                grunnbelop = beregningIPeriode.grunnbelop,
                grunnbelopMnd = beregningIPeriode.grunnbelopMnd
            )
        } ?: return null

        return YtelseMedGrunnlagDto(
            perioder = avkortinger
        )
    }
}