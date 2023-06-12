package no.nav.etterlatte.ytelseMedGrunnlag

import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.YtelseMedGrunnlagPeriodisertDto
import java.util.*

class YtelseMedGrunnlagService(private val avkortingRepository: AvkortingRepository) {

    fun hentYtelseMedGrunnlag(behandlingId: UUID): YtelseMedGrunnlagDto? {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)
        val avkortinger = avkorting?.avkortetYtelse?.map { avkortetYtelse ->

            val grunnlagIPeriode = avkorting.avkortingGrunnlag
                .filter { it.periode.fom <= avkortetYtelse.periode.fom }
                .maxBy { it.periode.fom }

            val grunnbeloep = GrunnbeloepRepository.hentGjeldendeGrunnbeloep(avkortetYtelse.periode.fom)

            YtelseMedGrunnlagPeriodisertDto(
                periode = avkortetYtelse.periode,
                ytelseEtterAvkorting = avkortetYtelse.ytelseEtterAvkorting,
                avkortingsbeloep = avkortetYtelse.avkortingsbeloep,
                ytelseFoerAvkorting = avkortetYtelse.ytelseFoerAvkorting,
                aarsinntekt = grunnlagIPeriode.aarsinntekt,
                fratrekkInnAar = grunnlagIPeriode.fratrekkInnAar,
                grunnbelop = grunnbeloep.grunnbeloep,
                grunnbelopMnd = grunnbeloep.grunnbeloepPerMaaned
            )
        } ?: return null

        return YtelseMedGrunnlagDto(
            perioder = avkortinger
        )
    }
}