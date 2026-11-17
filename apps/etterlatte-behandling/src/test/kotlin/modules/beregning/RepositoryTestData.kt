package no.nav.etterlatte.modules.beregning

import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.modules.beregning.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.modules.beregning.avkorting.AvkortetYtelse
import no.nav.etterlatte.modules.beregning.avkorting.AvkortetYtelseType
import no.nav.etterlatte.modules.beregning.avkorting.Avkortingsperiode
import no.nav.etterlatte.modules.beregning.avkorting.BenyttetInntektInnvilgetPeriode
import no.nav.etterlatte.modules.beregning.avkorting.ForventetInntekt
import no.nav.etterlatte.modules.beregning.avkorting.Inntektsavkorting
import no.nav.etterlatte.modules.beregning.avkorting.MaanedInnvilget
import no.nav.etterlatte.modules.beregning.avkorting.OverstyrtInnvilgaMaanederAarsak
import no.nav.etterlatte.modules.beregning.avkorting.Restanse
import no.nav.etterlatte.modules.beregning.avkorting.YtelseFoerAvkorting
import no.nav.etterlatte.modules.beregning.sanksjon.LagreSanksjon
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

val bruker = simpleSaksbehandler()

fun avkortinggrunnlag(
    id: UUID = UUID.randomUUID(),
    inntektTom: Int = 100000,
    fratrekkInnAar: Int = 10000,
    inntektUtlandTom: Int = 0,
    fratrekkInnAarUtland: Int = 0,
    innvilgaMaaneder: Int = 12,
    periode: Periode = Periode(fom = YearMonth.of(2024, 1), tom = null),
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
    overstyrtInnvilgaMaanederAarsak: OverstyrtInnvilgaMaanederAarsak? = null,
    overstyrtInnvilgaMaanederBegrunnelse: String? = null,
) = ForventetInntekt(
    id = id,
    periode = periode,
    inntektTom = inntektTom,
    fratrekkInnAar = fratrekkInnAar,
    inntektUtlandTom = inntektUtlandTom,
    fratrekkInnAarUtland = fratrekkInnAarUtland,
    innvilgaMaaneder = innvilgaMaaneder,
    spesifikasjon = "Spesifikasjon",
    kilde = kilde,
    overstyrtInnvilgaMaanederAarsak = overstyrtInnvilgaMaanederAarsak,
    overstyrtInnvilgaMaanederBegrunnelse = overstyrtInnvilgaMaanederBegrunnelse,
    inntektInnvilgetPeriode =
        BenyttetInntektInnvilgetPeriode(
            verdi = inntektTom - fratrekkInnAar + inntektUtlandTom - fratrekkInnAarUtland,
            tidspunkt = Tidspunkt.now(),
            regelResultat = "".toJsonNode(),
            kilde = Grunnlagsopplysning.RegelKilde("", Tidspunkt.now(), ""),
        ),
    maanederInnvilget =
        (periode.fom.monthValue..(periode.tom?.monthValue ?: 12)).map {
            MaanedInnvilget(YearMonth.of(periode.fom.year, it), true)
        },
    maanederInnvilgetRegelResultat = null,
)

fun aarsoppgjoer(
    aar: Int = 2024,
    fom: YearMonth = YearMonth.of(aar, 1),
    ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    avkortetYtelse: List<AvkortetYtelse> = emptyList(),
) = AarsoppgjoerLoepende(
    id = UUID.randomUUID(),
    aar = aar,
    fom = fom,
    ytelseFoerAvkorting = ytelseFoerAvkorting,
    inntektsavkorting = inntektsavkorting,
    avkortetYtelse = avkortetYtelse,
)

fun ytelseFoerAvkorting(
    beregning: Int = 100,
    periode: Periode = Periode(fom = YearMonth.of(2024, 1), tom = null),
    beregningsreferanse: UUID = UUID.randomUUID(),
) = YtelseFoerAvkorting(
    beregning = beregning,
    periode = periode,
    beregningsreferanse = beregningsreferanse,
)

fun avkortingsperiode(
    fom: YearMonth = YearMonth.of(2024, 1),
    tom: YearMonth? = null,
    avkorting: Int = 10000,
    inntektsgrunnlag: UUID = UUID.randomUUID(),
) = Avkortingsperiode(
    id = UUID.randomUUID(),
    Periode(fom = fom, tom = tom),
    avkorting = avkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
    inntektsgrunnlag = inntektsgrunnlag,
)

fun restanse(
    totalRestanse: Int = 100,
    fordeltRestanse: Int = 100,
) = Restanse(
    id = UUID.randomUUID(),
    totalRestanse = totalRestanse,
    fordeltRestanse = fordeltRestanse,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
)

fun avkortetYtelse(
    id: UUID = UUID.randomUUID(),
    type: AvkortetYtelseType = AvkortetYtelseType.FORVENTET_INNTEKT,
    ytelseEtterAvkorting: Int = 50,
    restanse: Int? = 50,
    ytelseEtterAvkortingFoerRestanse: Int = 100,
    avkortingsbeloep: Int = 200,
    ytelseFoerAvkorting: Int = 300,
    periode: Periode = Periode(fom = YearMonth.of(2024, 1), tom = null),
    inntektsgrunnlag: UUID? = UUID.randomUUID(),
    sanksjon: SanksjonertYtelse? = null,
) = AvkortetYtelse(
    id = id,
    type = type,
    periode = periode,
    ytelseEtterAvkorting = ytelseEtterAvkorting,
    restanse = restanse?.let { restanse(fordeltRestanse = it) },
    ytelseEtterAvkortingFoerRestanse = ytelseEtterAvkortingFoerRestanse,
    avkortingsbeloep = avkortingsbeloep,
    ytelseFoerAvkorting = ytelseFoerAvkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
    inntektsgrunnlag = inntektsgrunnlag,
    sanksjon = sanksjon,
)

fun beregningsperiode(
    datoFOM: YearMonth = YearMonth.now(),
    datoTOM: YearMonth? = null,
    utbetaltBeloep: Int = 3000,
    trygdetid: Int = 40,
    grunnbeloep: Int = 100_000,
) = Beregningsperiode(
    datoFOM = datoFOM,
    datoTOM = datoTOM,
    utbetaltBeloep = utbetaltBeloep,
    soeskenFlokk = listOf(HELSOESKEN_FOEDSELSNUMMER.value),
    grunnbelop = grunnbeloep,
    trygdetid = trygdetid,
    regelResultat = mapOf("regel" to "resultat").toObjectNode(),
    regelVersjon = "1",
    regelverk = Regelverk.fraDato(datoFOM.atDay(1)),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
)

fun BeregningsMetode.toGrunnlag() = BeregningsMetodeBeregningsgrunnlag(this, null)

fun lagreSanksjon(
    id: UUID? = null,
    sakId: SakId = randomSakId(),
    fom: LocalDate = LocalDate.of(2024, 1, 1),
    tom: LocalDate? = LocalDate.of(2024, 2, 1),
    beskrivelse: String = "Ikke i jobb",
    type: SanksjonType = SanksjonType.STANS,
) = LagreSanksjon(
    id = id,
    sakId = sakId,
    type = type,
    fom = fom,
    tom = tom,
    beskrivelse = beskrivelse,
)
