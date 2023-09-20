package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagDto
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.UUID
import no.nav.etterlatte.libs.common.beregning.BeregningDTO as CommonBeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode as CommonBeregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype as CommonBeregningstype

enum class Beregningstype {
    BP,
    OMS,
    ;

    companion object {
        fun fraDtoType(dto: CommonBeregningstype) =
            when (dto) {
                CommonBeregningstype.BP -> BP
                CommonBeregningstype.OMS -> OMS
            }
    }
}

data class Beregning(
    val beregningId: UUID,
    val behandlingId: UUID,
    val type: Beregningstype,
    val beregnetDato: Tidspunkt,
    val beregningsperioder: List<Beregningsperiode>,
) {
    companion object {
        fun fraBeregningDTO(dto: CommonBeregningDTO) =
            Beregning(
                beregningId = dto.beregningId,
                behandlingId = dto.behandlingId,
                type = Beregningstype.fraDtoType(dto.type),
                beregnetDato = dto.beregnetDato,
                beregningsperioder = dto.beregningsperioder.map(Beregningsperiode::fraBeregningsperiodeDTO),
            )
    }
}

data class Beregningsperiode(
    val datoFOM: YearMonth,
    val datoTOM: YearMonth?,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<String>?,
    val grunnbelopMnd: Int,
    val grunnbelop: Int,
    val trygdetid: Int,
) {
    companion object {
        fun fraBeregningsperiodeDTO(dto: CommonBeregningsperiode) =
            Beregningsperiode(
                datoFOM = dto.datoFOM,
                datoTOM = dto.datoTOM,
                utbetaltBeloep = dto.utbetaltBeloep,
                soeskenFlokk = dto.soeskenFlokk,
                grunnbelopMnd = dto.grunnbelopMnd,
                grunnbelop = dto.grunnbelop,
                trygdetid = dto.trygdetid,
            )
    }
}

data class Avkorting(
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val avkortetYtelse: List<AvkortetYtelse>,
) {
    companion object {
        fun fraDTO(dto: AvkortingDto) =
            Avkorting(
                avkortingGrunnlag = dto.avkortingGrunnlag.map { AvkortingGrunnlag.fraDTO(it) },
                avkortetYtelse = dto.avkortetYtelse.map { AvkortetYtelse.fraDTO(it) },
            )
    }
}

data class AvkortingGrunnlag(
    val fom: YearMonth,
    val tom: YearMonth?,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val relevanteMaanederInnAar: Int?,
    val spesifikasjon: String,
) {
    companion object {
        fun fraDTO(dto: AvkortingGrunnlagDto) =
            AvkortingGrunnlag(
                fom = dto.fom,
                tom = dto.tom,
                aarsinntekt = dto.aarsinntekt,
                fratrekkInnAar = dto.fratrekkInnAar,
                relevanteMaanederInnAar = dto.relevanteMaanederInnAar,
                spesifikasjon = dto.spesifikasjon,
            )
    }
}

data class AvkortetYtelse(
    val fom: YearMonth,
    val tom: YearMonth?,
    val ytelseFoerAvkorting: Int,
    val avkortingsbeloep: Int,
    val ytelseEtterAvkorting: Int,
    val restanse: Int,
) {
    companion object {
        fun fraDTO(dto: AvkortetYtelseDto) =
            AvkortetYtelse(
                fom = dto.fom,
                tom = dto.tom,
                ytelseFoerAvkorting = dto.ytelseFoerAvkorting,
                avkortingsbeloep = dto.avkortingsbeloep,
                ytelseEtterAvkorting = dto.ytelseEtterAvkorting,
                restanse = dto.restanse,
            )
    }
}
