package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.beregning.Avkorting
import no.nav.etterlatte.beregning.AvkortingGrunnlag
import no.nav.etterlatte.beregning.BeregnetAvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.InntektAvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.AvdoedForelder
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.token.Saksbehandler
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

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
    beregningEtterAvkorting = emptyList()
)

fun avkortinggrunnlag(
    aarsinntekt: Int = 100000
) = AvkortingGrunnlag(
    periode = Periode(fom = YearMonth.now(), tom = null),
    aarsinntekt = aarsinntekt,
    gjeldendeAar = 2023,
    spesifikasjon = "Spesifikasjon",
    beregnetAvkorting = emptyList()
)

fun inntektAvkortingGrunnlag(inntekt: Int = 500000) = InntektAvkortingGrunnlag(
    inntekt = FaktumNode(verdi = inntekt, "", "")
)

fun beregnetAvkortingGrunnlag() = BeregnetAvkortingGrunnlag(
    Periode(fom = YearMonth.now(), tom = null),
    avkorting = 10000,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode()
)