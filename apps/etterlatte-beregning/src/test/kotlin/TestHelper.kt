package no.nav.etterlatte.beregning.regler

import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingGrunnlag
import no.nav.etterlatte.avkorting.Avkortingsperiode
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
    avkortingGrunnlag: List<AvkortingGrunnlag> = emptyList(),
    avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    avkortetYtelse: List<AvkortetYtelse> = emptyList()
) = Avkorting(
    avkortingGrunnlag = avkortingGrunnlag,
    avkortingsperioder = avkortingsperioder,
    avkortetYtelse = avkortetYtelse
)

fun avkortinggrunnlag(
    id: UUID = UUID.randomUUID(),
    aarsinntekt: Int = 100000,
    fratrekkInnUt: Int = 10000,
    relevanteMaaneder: Int = 12,
    periode: Periode = Periode(fom = YearMonth.now(), tom = null),
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123456")
) = AvkortingGrunnlag(
    id = id,
    periode = periode,
    aarsinntekt = aarsinntekt,
    fratrekkInnAar = fratrekkInnUt,
    relevanteMaanederInnAar = relevanteMaaneder,
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
            relevanteMaaneder = Beregningstall(relevanteMaaneder)
        ),
        kilde = "",
        beskrivelse = ""
    )
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

fun behandling(
    id: UUID = UUID.randomUUID(),
    sak: Long = 123,
    sakType: SakType = SakType.OMSTILLINGSSTOENAD,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt: YearMonth = YearMonth.of(2023, 1)
) = DetaljertBehandling(
    id = id,
    sak = sak,
    sakType = sakType,
    behandlingOpprettet = LocalDateTime.now(),
    sistEndret = LocalDateTime.now(),
    soeknadMottattDato = null,
    innsender = null,
    soeker = "diam",
    gjenlevende = listOf(),
    avdoed = listOf(),
    soesken = listOf(),
    gyldighetsproeving = null,
    status = BehandlingStatus.VILKAARSVURDERT,
    behandlingType = behandlingType,
    virkningstidspunkt = VirkningstidspunktTestData.virkningstidsunkt(virkningstidspunkt),
    kommerBarnetTilgode = null,
    revurderingsaarsak = null,
    prosesstype = Prosesstype.MANUELL,
    utenlandstilsnitt = null,
    boddEllerArbeidetUtlandet = null
)