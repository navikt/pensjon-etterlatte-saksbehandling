package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.avkorting.Avkortingsperiode
import no.nav.etterlatte.avkorting.regler.AvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.AvdoedForelder
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.token.Saksbehandler
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

val REGEL_PERIODE = RegelPeriode(LocalDate.of(2023, 1, 1))

const val FNR_1 = "11057523044"
const val FNR_2 = "19040550081"
const val FNR_3 = "24014021406"

const val MAKS_TRYGDETID: Int = 40

fun barnepensjonGrunnlag(
    soeskenKull: List<String> = emptyList(),
    trygdeTid: Beregningstall = Beregningstall(MAKS_TRYGDETID),
    institusjonsopphold: InstitusjonsoppholdBeregningsgrunnlag? = null
) = BarnepensjonGrunnlag(
    soeskenKull = FaktumNode(soeskenKull.map { Folkeregisteridentifikator.of(it) }, kilde, "søskenkull"),
    avdoedForelder = FaktumNode(AvdoedForelder(trygdetid = trygdeTid), kilde, "trygdetid"),
    institusjonsopphold = FaktumNode(institusjonsopphold, kilde, "institusjonsopphold")
)

fun Double.toBeregningstall(
    decimals: Int = Beregningstall.DESIMALER_DELBEREGNING,
    roundingMode: RoundingMode = RoundingMode.UNNECESSARY
) = Beregningstall(this).setScale(decimals, roundingMode)

fun Int.toBeregningstall(
    decimals: Int = Beregningstall.DESIMALER_DELBEREGNING,
    roundingMode: RoundingMode = RoundingMode.UNNECESSARY
) = Beregningstall(this).setScale(decimals, roundingMode)

val bruker = Saksbehandler("token", "ident", null)

fun avkorting(
    behandlingId: UUID = UUID.randomUUID(),
    avkortingGrunnlag: List<AvkortingGrunnlag> = emptyList(),
    avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    avkortetYtelse: List<AvkortetYtelse> = emptyList()
) = Avkorting(
    behandlingId = behandlingId,
    avkortingGrunnlag = avkortingGrunnlag,
    avkortingsperioder = avkortingsperioder,
    avkortetYtelse = avkortetYtelse
)

fun avkortinggrunnlag(
    aarsinntekt: Int = 100000,
    periode: Periode = Periode(fom = YearMonth.now(), tom = null),
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123456")
) = AvkortingGrunnlag(
    periode = periode,
    aarsinntekt = aarsinntekt,
    fratrekkInnUt = 10000,
    spesifikasjon = "Spesifikasjon",
    kilde = kilde
)

fun inntektAvkortingGrunnlag(inntekt: Int = 500000) = InntektAvkortingGrunnlag(
    inntekt = FaktumNode(verdi = inntekt, "", "")
)

fun avkortingsperiode(
    fom: YearMonth = YearMonth.now(),
    tom: YearMonth? = null,
    avkorting: Int = 10000
) = Avkortingsperiode(
    Periode(fom = fom, tom = tom),
    avkorting = avkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1")
)

fun avkortetYtelseGrunnlag(beregning: Int, avkorting: Int) = AvkortetYtelseGrunnlag(
    beregning = FaktumNode(verdi = beregning, "", ""),
    avkorting = FaktumNode(verdi = avkorting, "", "")
)

fun avkortetYtelse(
    ytelseEtterAvkorting: Int = 100,
    avkortingsbeloep: Int = 200,
    ytelseFoerAvkorting: Int = 300,
    periode: Periode = Periode(
        fom = YearMonth.now(),
        tom = null
    )
) = AvkortetYtelse(
    periode = periode,
    ytelseEtterAvkorting = ytelseEtterAvkorting,
    avkortingsbeloep = avkortingsbeloep,
    ytelseFoerAvkorting = ytelseFoerAvkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1")
)

fun beregning(
    beregninger: List<Beregningsperiode> = listOf(beregningsperiode())
) = Beregning(
    beregningId = UUID.randomUUID(),
    behandlingId = UUID.randomUUID(),
    type = Beregningstype.OMS,
    beregningsperioder = beregninger,
    beregnetDato = Tidspunkt.now(),
    grunnlagMetadata = Metadata(sakId = 123L, versjon = 1L)
)

fun beregningsperiode(
    datoFOM: YearMonth = YearMonth.now(),
    datoTOM: YearMonth? = null,
    utbetaltBeloep: Int = 3000
) = Beregningsperiode(
    datoFOM = datoFOM,
    datoTOM = datoTOM,
    utbetaltBeloep = utbetaltBeloep,
    soeskenFlokk = listOf(FNR_1),
    grunnbelopMnd = 10_000,
    grunnbelop = 100_000,
    trygdetid = 40,
    regelResultat = mapOf("regel" to "resultat").toObjectNode(),
    regelVersjon = "1",
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1")
)