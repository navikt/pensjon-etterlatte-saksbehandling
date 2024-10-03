package no.nav.etterlatte.libs.common.beregning

import Regelverk
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.UUID

data class BeregningDTO(
    val beregningId: UUID,
    val behandlingId: UUID,
    val type: Beregningstype,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: Tidspunkt,
    val grunnlagMetadata: Metadata,
    val overstyrBeregning: OverstyrBeregningDTO?,
)

data class Beregningsperiode(
    val id: UUID? = null,
    val datoFOM: YearMonth,
    val datoTOM: YearMonth? = null,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<String>? = null,
    val institusjonsopphold: InstitusjonsoppholdBeregningsgrunnlag? = null,
    val grunnbelopMnd: Int,
    val grunnbelop: Int,
    val trygdetid: Int,
    val trygdetidForIdent: String? = null,
    val beregningsMetode: BeregningsMetode? = null,
    val samletNorskTrygdetid: Int? = null,
    val samletTeoretiskTrygdetid: Int? = null,
    val broek: IntBroek? = null,
    val avdoedeForeldre: List<String?>? = null,
    val regelResultat: JsonNode? = null,
    val regelVersjon: String? = null,
    val regelverk: Regelverk? = null,
    val kunEnJuridiskForelder: Boolean = false,
    val kilde: Grunnlagsopplysning.RegelKilde? = null,
)

data class OverstyrBeregningDTO(
    val beskrivelse: String,
    val kategori: OverstyrtBeregningKategori,
)

data class AvkortingFrontend(
    val avkortingGrunnlag: List<AvkortingGrunnlagFrontend>,
    val avkortetYtelse: List<AvkortetYtelseDto>,
    val tidligereAvkortetYtelse: List<AvkortetYtelseDto> = emptyList(),
)

data class AvkortingGrunnlagFrontend(
    val aar: Int,
    val fraVirk: AvkortingGrunnlagDto?,
    val historikk: List<AvkortingGrunnlagDto>,
)

data class AvkortingDto(
    val avkortingGrunnlag: List<AvkortingGrunnlagDto>, // TODO kan "flyttes" inn i avkortetYteleDto?
    val avkortetYtelse: List<AvkortetYtelseDto>,
    val tidligereAvkortetYtelse: List<AvkortetYtelseDto> = emptyList(), // TODO Fjern
)

data class AvkortingGrunnlagDto(
    val id: UUID,
    val fom: YearMonth,
    val tom: YearMonth?,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val inntektUtland: Int,
    val fratrekkInnAarUtland: Int,
    val relevanteMaanederInnAar: Int,
    val spesifikasjon: String,
    val kilde: AvkortingGrunnlagKildeDto,
)

data class AvkortingGrunnlagLagreDto(
    val id: UUID = UUID.randomUUID(),
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val inntektUtland: Int,
    val fratrekkInnAarUtland: Int,
    val spesifikasjon: String,
    val fom: YearMonth,
)

data class AvkortingGrunnlagKildeDto(
    val tidspunkt: String,
    val ident: String,
)

data class AvkortetYtelseDto(
    val id: UUID? = null,
    val fom: YearMonth,
    val tom: YearMonth?,
    val type: String = "",
    val ytelseFoerAvkorting: Int,
    val avkortingsbeloep: Int,
    val ytelseEtterAvkorting: Int,
    val restanse: Int,
    val sanksjon: SanksjonertYtelse?,
)

data class YtelseMedGrunnlagDto(
    val perioder: List<YtelseMedGrunnlagPeriodisertDto>,
    val inntektForNesteAar: Boolean,
)

data class YtelseMedGrunnlagPeriodisertDto(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val restanse: Int,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val relevanteMaanederInnAar: Int,
    val trygdetid: Int,
    val grunnbelop: Int,
    val grunnbelopMnd: Int,
    val beregningsMetode: BeregningsMetode?,
    val sanksjon: SanksjonertYtelse?,
    val institusjonsopphold: InstitusjonsoppholdBeregningsgrunnlag?,
)

enum class OverstyrtBeregningKategori {
    UKJENT_AVDOED,
    AVKORTING_UFOERETRYGD,
    FENGSELSOPPHOLD,
    UKJENT_KATEGORI, // for å kunne håndtere tidligere overstyringer som ikke har kategori
}
