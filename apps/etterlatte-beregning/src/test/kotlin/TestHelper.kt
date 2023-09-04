package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.avkorting.Avkortingsperiode
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.avkorting.Restanse
import no.nav.etterlatte.avkorting.YtelseFoerAvkorting
import no.nav.etterlatte.avkorting.regler.AvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlagWrapper
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.AvdoedForelder
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
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
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.token.Saksbehandler
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
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
    ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    avkortetYtelseAar: List<AvkortetYtelse> = emptyList()
) = Avkorting(
    aarsoppgjoer = aarsoppgjoer(
        ytelseFoerAvkorting,
        inntektsavkorting,
        avkortetYtelseAar,
    )
)

fun avkortinggrunnlag(
    id: UUID = UUID.randomUUID(),
    aarsinntekt: Int = 100000,
    fratrekkInnAar: Int = 10000,
    relevanteMaanederInnAar: Int = 12,
    periode: Periode = Periode(fom = YearMonth.now(), tom = null),
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
    virkningstidspunkt: YearMonth = YearMonth.now()
) = AvkortingGrunnlag(
    id = id,
    periode = periode,
    aarsinntekt = aarsinntekt,
    fratrekkInnAar = fratrekkInnAar,
    relevanteMaanederInnAar = relevanteMaanederInnAar,
    spesifikasjon = "Spesifikasjon",
    kilde = kilde
)

fun inntektAvkortingGrunnlag(
    inntekt: Int = 500000,
    fratrekkInnUt: Int = 0,
    relevanteMaaneder: Int = 12
) = InntektAvkortingGrunnlagWrapper(
    inntektAvkortingGrunnlag = FaktumNode(
        verdi = InntektAvkortingGrunnlag(
            inntekt = Beregningstall(inntekt),
            fratrekkInnUt = Beregningstall(fratrekkInnUt),
            relevanteMaaneder = Beregningstall(relevanteMaaneder),
            grunnlagId = UUID.randomUUID()
        ),
        kilde = "",
        beskrivelse = ""
    )
)

fun aarsoppgjoer(
    ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    avkortetYtelseAar: List<AvkortetYtelse> = emptyList()
) = Aarsoppgjoer(
    ytelseFoerAvkorting = ytelseFoerAvkorting,
    inntektsavkorting = inntektsavkorting,
    avkortetYtelseAar = avkortetYtelseAar
)

fun ytelseFoerAvkorting(
    beregning: Int = 100,
    periode: Periode = Periode(fom = YearMonth.of(2023, 1), tom = null),
    beregningsreferanse: UUID = UUID.randomUUID()
) = YtelseFoerAvkorting(
    beregning = beregning,
    periode = periode,
    beregningsreferanse = beregningsreferanse
)

fun avkortingsperiode(
    fom: YearMonth = YearMonth.now(),
    tom: YearMonth? = null,
    avkorting: Int = 10000,
    inntektsgrunnlag: UUID = UUID.randomUUID()
) = Avkortingsperiode(
    id = UUID.randomUUID(),
    Periode(fom = fom, tom = tom),
    avkorting = avkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
    inntektsgrunnlag = inntektsgrunnlag
)

fun restanse(
    totalRestanse: Int = 100,
    fordeltRestanse: Int = 100
) = Restanse(
    id = UUID.randomUUID(),
    totalRestanse = totalRestanse,
    fordeltRestanse = fordeltRestanse,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1")
)

fun avkortetYtelseGrunnlag(beregning: Int, avkorting: Int, fordeltRestanse: Int = 0) = AvkortetYtelseGrunnlag(
    beregning = FaktumNode(verdi = beregning, "", ""),
    avkorting = FaktumNode(verdi = avkorting, "", ""),
    fordeltRestanse = FaktumNode(verdi = fordeltRestanse, "", "")
)

fun avkortetYtelse(
    id: UUID = UUID.randomUUID(),
    type: AvkortetYtelseType = AvkortetYtelseType.INNTEKT,
    ytelseEtterAvkorting: Int = 50,
    restanse: Int? = 50,
    ytelseEtterAvkortingFoerRestanse: Int = 100,
    avkortingsbeloep: Int = 200,
    ytelseFoerAvkorting: Int = 300,
    periode: Periode = Periode(
        fom = YearMonth.now(),
        tom = null
    ),
    inntektsgrunnlag: UUID? = UUID.randomUUID()
) = AvkortetYtelse(
    id = id,
    type = type,
    periode = periode,
    ytelseEtterAvkorting = ytelseEtterAvkorting,
    restanse = restanse?.let {
        restanse(
            fordeltRestanse = restanse
        )
    },
    ytelseEtterAvkortingFoerRestanse = ytelseEtterAvkortingFoerRestanse,
    avkortingsbeloep = avkortingsbeloep,
    ytelseFoerAvkorting = ytelseFoerAvkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
    inntektsgrunnlag = inntektsgrunnlag
)

fun beregning(
    beregningId: UUID = UUID.randomUUID(),
    beregninger: List<Beregningsperiode> = listOf(beregningsperiode())
) = Beregning(
    beregningId = beregningId,
    behandlingId = UUID.randomUUID(),
    type = Beregningstype.OMS,
    beregningsperioder = beregninger,
    beregnetDato = Tidspunkt.now(),
    grunnlagMetadata = Metadata(sakId = 123L, versjon = 1L)
)

fun beregningsperiode(
    datoFOM: YearMonth = YearMonth.now(),
    datoTOM: YearMonth? = null,
    utbetaltBeloep: Int = 3000,
    trygdetid: Int = 40,
    grunnbeloep: Int = 100_000,
    grunnbeloepMnd: Int = 10_000
) = Beregningsperiode(
    datoFOM = datoFOM,
    datoTOM = datoTOM,
    utbetaltBeloep = utbetaltBeloep,
    soeskenFlokk = listOf(FNR_1),
    grunnbelopMnd = grunnbeloepMnd,
    grunnbelop = grunnbeloep,
    trygdetid = trygdetid,
    regelResultat = mapOf("regel" to "resultat").toObjectNode(),
    regelVersjon = "1",
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1")
)

fun behandling(
    id: UUID = UUID.randomUUID(),
    sak: Long = 123,
    sakType: SakType = SakType.OMSTILLINGSSTOENAD,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt: Virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2023, 1))
) = DetaljertBehandling(
    id = id,
    sak = sak,
    sakType = sakType,
    behandlingOpprettet = LocalDateTime.now(),
    soeknadMottattDato = null,
    innsender = null,
    soeker = "12312312321",
    gjenlevende = listOf(),
    avdoed = listOf(),
    soesken = listOf(),
    status = BehandlingStatus.VILKAARSVURDERT,
    behandlingType = behandlingType,
    virkningstidspunkt = virkningstidspunkt,
    revurderingsaarsak = null,
    prosesstype = Prosesstype.MANUELL,
    boddEllerArbeidetUtlandet = null,
    revurderingInfo = null,
    enhet = "1111"
)