package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.beregning.AvkortetYtelse
import no.nav.etterlatte.beregning.Avkorting
import no.nav.etterlatte.beregning.AvkortingGrunnlag
import no.nav.etterlatte.beregning.BeregnetAvkortingGrunnlag
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.regler.avkorting.AvkortetYtelseGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.InntektAvkortingGrunnlag
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
    trygdeTid: Beregningstall = Beregningstall(MAKS_TRYGDETID)
) = BarnepensjonGrunnlag(
    soeskenKull = FaktumNode(soeskenKull.map { Folkeregisteridentifikator.of(it) }, kilde, "søskenkull"),
    avdoedForelder = FaktumNode(AvdoedForelder(trygdetid = trygdeTid), kilde, "trygdetid")
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
    behandlingId: UUID = UUID.randomUUID()
) = Avkorting(
    behandlingId = behandlingId,
    avkortingGrunnlag = emptyList(),
    avkortetYtelse = emptyList()
)

fun avkortinggrunnlag(
    aarsinntekt: Int = 100000,
    beregnetAvkorting: List<BeregnetAvkortingGrunnlag> = emptyList()
) = AvkortingGrunnlag(
    periode = Periode(fom = YearMonth.now(), tom = null),
    aarsinntekt = aarsinntekt,
    gjeldendeAar = 2023,
    spesifikasjon = "Spesifikasjon",
    kilde = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
    beregnetAvkorting = beregnetAvkorting
)

fun inntektAvkortingGrunnlag(inntekt: Int = 500000) = InntektAvkortingGrunnlag(
    inntekt = FaktumNode(verdi = inntekt, "", "")
)

fun beregnetAvkortingGrunnlag(
    fom: YearMonth = YearMonth.now(),
    tom: YearMonth? = null,
    avkorting: Int = 10000
) = BeregnetAvkortingGrunnlag(
    Periode(fom = fom, tom = tom),
    avkorting = avkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode()
)

fun avkortetYtelseGrunnlag(bruttoYtelse: Int, avkorting: Int) = AvkortetYtelseGrunnlag(
    fom = YearMonth.now(),
    bruttoYtelse = FaktumNode(verdi = bruttoYtelse, "", ""),
    avkorting = FaktumNode(verdi = avkorting, "", "")
)

fun avkortetYtelse() = AvkortetYtelse(
    periode = Periode(fom = YearMonth.now(), tom = null),
    ytelseEtterAvkorting = 100,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode()
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
    regelVersjon = "1"
)