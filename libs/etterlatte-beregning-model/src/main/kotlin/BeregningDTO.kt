package no.nav.etterlatte.libs.common.beregning

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
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
    val harForeldreloessats: Boolean? = null,
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
    val avkortingGrunnlag: List<AvkortingGrunnlagDto>,
    val avkortetYtelse: List<AvkortetYtelseDto>,
)

data class AvkortingGrunnlagDto(
    val id: UUID,
    val fom: YearMonth,
    val tom: YearMonth?,
    val inntektTom: Int,
    val fratrekkInnAar: Int,
    val inntektUtlandTom: Int,
    val fratrekkInnAarUtland: Int,
    val innvilgaMaaneder: Int,
    val spesifikasjon: String,
    val kilde: AvkortingGrunnlagKildeDto,
    val overstyrtInnvilgaMaaneder: AvkortingOverstyrtInnvilgaMaanederDto? = null,
)

data class AvkortingGrunnlagLagreDto(
    val id: UUID = UUID.randomUUID(),
    val inntektTom: Int,
    val fratrekkInnAar: Int,
    val inntektUtlandTom: Int,
    val fratrekkInnAarUtland: Int,
    val spesifikasjon: String,
    val fom: YearMonth,
    val overstyrtInnvilgaMaaneder: AvkortingOverstyrtInnvilgaMaanederDto? = null,
)

data class AvkortingOverstyrtInnvilgaMaanederDto(
    val antall: Int,
    val aarsak: String,
    val begrunnelse: String,
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

data class BeregningOgAvkortingDto(
    val perioder: List<BeregningOgAvkortingPeriodeDto>,
    val erInnvilgelsesaar: Boolean,
    // Hvis nytt beløp fra siste åpne periode er ulik den siste åpne perioden til forrige behandling
    val endringIUtbetalingVedVirk: Boolean,
)

data class BeregningOgAvkortingPeriodeDto(
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val restanse: Int,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val oppgittInntekt: Int,
    val fratrekkInnAar: Int,
    val innvilgaMaaneder: Int,
    val trygdetid: Int,
    val grunnbelop: Int,
    val grunnbelopMnd: Int,
    val beregningsMetode: BeregningsMetode?,
    val beregningsMetodeFraGrunnlag: BeregningsMetode?,
    val sanksjon: SanksjonertYtelse?,
    val institusjonsopphold: InstitusjonsoppholdBeregningsgrunnlag?,
    val erOverstyrtInnvilgaMaaneder: Boolean,
)

enum class OverstyrtBeregningKategori {
    UKJENT_AVDOED,
    AVKORTING_UFOERETRYGD,
    FENGSELSOPPHOLD,
    UKJENT_KATEGORI, // for å kunne håndtere tidligere overstyringer som ikke har kategori
}

data class InntektsjusteringAvkortingInfoRequest(
    val sakId: SakId,
    val aar: Int,
    val sisteBehandling: UUID,
)

data class InntektsjusteringAvkortingInfoResponse(
    val sakId: SakId,
    val aar: Int,
    val harInntektForAar: Boolean,
    val harSanksjon: Boolean,
)

data class AarligInntektsjusteringAvkortingRequest(
    val aar: Int,
    val forrigeBehandling: UUID,
    val nyBehandling: UUID,
)

data class MottattInntektsjusteringAvkortigRequest(
    val behandlingId: UUID,
    val virkningstidspunkt: YearMonth,
    val mottattInntektsjustering: MottattInntektsjustering,
)

data class AvkortingEtteropppgjoerRequest(
    val sisteIverksatteBehandling: UUID,
    val aar: Int,
)

data class EtteroppgjoerBeregnFaktiskInntektRequest(
    val sakId: SakId,
    val forbehandlingId: UUID,
    val sisteIverksatteBehandling: UUID,
    val fraOgMed: YearMonth,
    val tilOgMed: YearMonth,
    val loennsinntekt: Int,
    val afp: Int,
    val naeringsinntekt: Int,
    val utland: Int,
)
