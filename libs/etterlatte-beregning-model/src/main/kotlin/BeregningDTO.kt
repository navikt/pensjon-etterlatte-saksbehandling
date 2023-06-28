package no.nav.etterlatte.libs.common.beregning

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

data class BeregningDTO(
    val beregningId: UUID,
    val behandlingId: UUID,
    val type: Beregningstype,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: Tidspunkt,
    val grunnlagMetadata: Metadata
)

data class Beregningsperiode(
    val datoFOM: YearMonth,
    val datoTOM: YearMonth? = null,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<String>? = null,
    val institusjonsopphold: InstitusjonsoppholdBeregningsgrunnlag? = null,
    val grunnbelopMnd: Int,
    val grunnbelop: Int,
    val trygdetid: Int,
    val regelResultat: JsonNode? = null,
    val regelVersjon: String? = null,
    val kilde: Grunnlagsopplysning.RegelKilde? = null
)

data class AvkortingDto(
    val avkortingGrunnlag: List<AvkortingGrunnlagDto>,
    val avkortetYtelse: List<AvkortetYtelseDto>
)

data class AvkortingGrunnlagDto(
    val id: UUID = UUID.randomUUID(),
    val fom: YearMonth,
    val tom: YearMonth?,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val relevanteMaanederInnAar: Int?,
    val spesifikasjon: String,
    val kilde: AvkortingGrunnlagKildeDto?
)

data class AvkortingGrunnlagKildeDto(
    val tidspunkt: String,
    val ident: String
)

data class AvkortetYtelseDto(
    val fom: LocalDate,
    val tom: LocalDate?,
    val avkortingsbeloep: Int,
    val ytelseEtterAvkorting: Int
)

data class YtelseMedGrunnlagDto(
    val perioder: List<YtelseMedGrunnlagPeriodisertDto>
)

data class YtelseMedGrunnlagPeriodisertDto(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val grunnbelop: Int,
    val grunnbelopMnd: Int
)