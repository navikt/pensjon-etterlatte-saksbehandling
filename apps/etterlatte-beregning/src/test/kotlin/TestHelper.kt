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
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.beregning.OverstyrBeregning
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.SamletTrygdetidMedBeregningsMetode
import no.nav.etterlatte.libs.common.beregning.SanksjonType
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.testdata.behandling.VirkningstidspunktTestData
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.regler.Beregningstall
import no.nav.etterlatte.sanksjon.LagreSanksjon
import no.nav.etterlatte.sanksjon.Sanksjon
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

val REGEL_PERIODE = RegelPeriode(LocalDate.of(2023, 1, 1))
val STANDARDSAK = randomSakId()

const val MAKS_TRYGDETID: Int = 40

fun barnepensjonGrunnlag(
    soeskenKull: List<Folkeregisteridentifikator> = emptyList(),
    trygdeTid: Beregningstall = Beregningstall(MAKS_TRYGDETID),
    institusjonsopphold: InstitusjonsoppholdBeregningsgrunnlag? = null,
    avdoede: List<Folkeregisteridentifikator> = listOf(AVDOED_FOEDSELSNUMMER),
) = BarnepensjonGrunnlag(
    soeskenKull = FaktumNode(soeskenKull, kilde, "søskenkull"),
    avdoedesTrygdetid =
        FaktumNode(
            avdoede.map { AnvendtTrygdetid(BeregningsMetode.NASJONAL, trygdeTid, it.value) },
            kilde,
            "trygdetid",
        ),
    institusjonsopphold = FaktumNode(institusjonsopphold, kilde, "institusjonsopphold"),
    kunEnJuridiskForelder = FaktumNode(false, kilde, "kunEnJuridiskForelder"),
)

fun samletTrygdetid(
    beregningsMetode: BeregningsMetode,
    samletTrygdetidNorge: Beregningstall? = null,
    samletTrygdetidTeoretisk: Beregningstall? = null,
    broek: IntBroek? = null,
    ident: String,
) = FaktumNode(
    SamletTrygdetidMedBeregningsMetode(
        beregningsMetode = beregningsMetode,
        samletTrygdetidNorge = samletTrygdetidNorge,
        samletTrygdetidTeoretisk = samletTrygdetidTeoretisk,
        prorataBroek = broek,
        ident = ident,
    ),
    kilde,
    "trygdetid",
)

fun Double.toBeregningstall(
    decimals: Int = Beregningstall.DESIMALER_DELBEREGNING,
    roundingMode: RoundingMode = RoundingMode.UNNECESSARY,
) = Beregningstall(this).setScale(decimals, roundingMode)

fun Int.toBeregningstall(
    decimals: Int = Beregningstall.DESIMALER_DELBEREGNING,
    roundingMode: RoundingMode = RoundingMode.UNNECESSARY,
) = Beregningstall(this).setScale(decimals, roundingMode)

val bruker = simpleSaksbehandler()

fun avkorting(
    ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    avkortetYtelseAar: List<AvkortetYtelse> = emptyList(),
) = Avkorting(
    aarsoppgjoer =
        listOf(
            aarsoppgjoer(
                ytelseFoerAvkorting = ytelseFoerAvkorting,
                inntektsavkorting = inntektsavkorting,
                avkortetYtelseAar = avkortetYtelseAar,
            ),
        ),
)

fun avkortinggrunnlag(
    id: UUID = UUID.randomUUID(),
    aarsinntekt: Int = 100000,
    fratrekkInnAar: Int = 10000,
    fratrekkInnAarUtland: Int = 0,
    periode: Periode = Periode(fom = YearMonth.of(2024, 1), tom = null),
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
) = AvkortingGrunnlag(
    id = id,
    periode = periode,
    aarsinntekt = aarsinntekt,
    fratrekkInnAar = fratrekkInnAar,
    inntektUtland = 0,
    fratrekkInnAarUtland = fratrekkInnAarUtland,
    spesifikasjon = "Spesifikasjon",
    kilde = kilde,
)

fun avkortinggrunnlagLagre(
    id: UUID = UUID.randomUUID(),
    aarsinntekt: Int = 100000,
    fratrekkInnAar: Int = 10000,
    fom: YearMonth,
) = AvkortingGrunnlagLagreDto(
    id = id,
    aarsinntekt = aarsinntekt,
    fratrekkInnAar = fratrekkInnAar,
    inntektUtland = 0,
    fratrekkInnAarUtland = 0,
    spesifikasjon = "Spesifikasjon",
    fom = fom,
)

fun inntektAvkortingGrunnlag(
    inntekt: Int = 500000,
    fratrekkInnAar: Int = 0,
    inntektUtland: Int = 0,
    relevanteMaaneder: Int = 12,
) = InntektAvkortingGrunnlagWrapper(
    inntektAvkortingGrunnlag =
        FaktumNode(
            verdi =
                InntektAvkortingGrunnlag(
                    inntekt = Beregningstall(inntekt),
                    fratrekkInnAar = Beregningstall(fratrekkInnAar),
                    inntektUtland = Beregningstall(inntektUtland),
                    fratrekkInnAarUtland = Beregningstall(0),
                    relevanteMaaneder = Beregningstall(relevanteMaaneder),
                    grunnlagId = UUID.randomUUID(),
                ),
            kilde = "",
            beskrivelse = "",
        ),
)

fun aarsoppgjoer(
    aar: Int = 2024,
    forventaInnvilgaMaaneder: Int = 12,
    ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    avkortetYtelseAar: List<AvkortetYtelse> = emptyList(),
) = Aarsoppgjoer(
    id = UUID.randomUUID(),
    aar = aar,
    forventaInnvilgaMaaneder = forventaInnvilgaMaaneder,
    ytelseFoerAvkorting = ytelseFoerAvkorting,
    inntektsavkorting = inntektsavkorting,
    avkortetYtelseAar = avkortetYtelseAar,
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

fun avkortetYtelseGrunnlag(
    beregning: Int,
    avkorting: Int,
    fordeltRestanse: Int = 0,
    sanksjon: Sanksjon? = null,
) = AvkortetYtelseGrunnlag(
    beregning = FaktumNode(verdi = beregning, "", ""),
    avkorting = FaktumNode(verdi = avkorting, "", ""),
    fordeltRestanse = FaktumNode(verdi = fordeltRestanse, "", ""),
    sanksjon = FaktumNode(verdi = sanksjon, "", ""),
)

fun avkortetYtelse(
    id: UUID = UUID.randomUUID(),
    type: AvkortetYtelseType = AvkortetYtelseType.FORVENTET_INNTEKT,
    ytelseEtterAvkorting: Int = 50,
    restanse: Int? = 50,
    ytelseEtterAvkortingFoerRestanse: Int = 100,
    avkortingsbeloep: Int = 200,
    ytelseFoerAvkorting: Int = 300,
    periode: Periode =
        Periode(
            fom = YearMonth.of(2024, 1),
            tom = null,
        ),
    inntektsgrunnlag: UUID? = UUID.randomUUID(),
    sanksjon: SanksjonertYtelse? = null,
) = AvkortetYtelse(
    id = id,
    type = type,
    periode = periode,
    ytelseEtterAvkorting = ytelseEtterAvkorting,
    restanse =
        restanse?.let {
            restanse(
                fordeltRestanse = restanse,
            )
        },
    ytelseEtterAvkortingFoerRestanse = ytelseEtterAvkortingFoerRestanse,
    avkortingsbeloep = avkortingsbeloep,
    ytelseFoerAvkorting = ytelseFoerAvkorting,
    tidspunkt = Tidspunkt.now(),
    regelResultat = "".toJsonNode(),
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
    inntektsgrunnlag = inntektsgrunnlag,
    sanksjon = sanksjon,
)

fun beregning(
    beregningId: UUID = UUID.randomUUID(),
    beregninger: List<Beregningsperiode> = listOf(beregningsperiode()),
    overstyrBeregning: OverstyrBeregning? = null,
) = Beregning(
    beregningId = beregningId,
    behandlingId = UUID.randomUUID(),
    type = Beregningstype.OMS,
    beregningsperioder = beregninger,
    beregnetDato = Tidspunkt.now(),
    grunnlagMetadata = Metadata(sakId = STANDARDSAK, versjon = 1L),
    overstyrBeregning = overstyrBeregning,
)

fun beregningsperiode(
    datoFOM: YearMonth = YearMonth.now(),
    datoTOM: YearMonth? = null,
    utbetaltBeloep: Int = 3000,
    trygdetid: Int = 40,
    grunnbeloep: Int = 100_000,
    grunnbeloepMnd: Int = 10_000,
) = Beregningsperiode(
    datoFOM = datoFOM,
    datoTOM = datoTOM,
    utbetaltBeloep = utbetaltBeloep,
    soeskenFlokk = listOf(HELSOESKEN_FOEDSELSNUMMER.value),
    grunnbelopMnd = grunnbeloepMnd,
    grunnbelop = grunnbeloep,
    trygdetid = trygdetid,
    regelResultat = mapOf("regel" to "resultat").toObjectNode(),
    regelVersjon = "1",
    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
)

fun behandling(
    id: UUID = UUID.randomUUID(),
    sak: SakId = STANDARDSAK,
    sakType: SakType = SakType.OMSTILLINGSSTOENAD,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt: Virkningstidspunkt? = VirkningstidspunktTestData.virkningstidsunkt(YearMonth.of(2024, 1)),
    status: BehandlingStatus = BehandlingStatus.BEREGNET,
) = DetaljertBehandling(
    id = id,
    sak = sak,
    sakType = sakType,
    soeker = "12312312321",
    status = status,
    behandlingType = behandlingType,
    virkningstidspunkt = virkningstidspunkt,
    revurderingsaarsak = null,
    prosesstype = Prosesstype.MANUELL,
    boddEllerArbeidetUtlandet = null,
    utlandstilknytning = null,
    revurderingInfo = null,
    kilde = Vedtaksloesning.GJENNY,
    sendeBrev = true,
    opphoerFraOgMed = null,
    relatertBehandlingId = null,
)

fun BeregningsMetode.toGrunnlag() = BeregningsMetodeBeregningsgrunnlag(this, null)

fun sanksjon(
    id: UUID? = UUID.randomUUID(),
    behandlingId: UUID = UUID.randomUUID(),
    sakId: SakId = STANDARDSAK,
    fom: YearMonth = YearMonth.of(2024, 1),
    tom: YearMonth? = YearMonth.of(2024, 2),
    type: SanksjonType = SanksjonType.STANS,
    beskrivelse: String = "Ikke i jobb",
) = Sanksjon(
    id = id,
    behandlingId = behandlingId,
    sakId = sakId,
    type = type,
    fom = fom,
    tom = tom,
    opprettet = Grunnlagsopplysning.Saksbehandler.create("A12345"),
    endret = Grunnlagsopplysning.Saksbehandler.create("A12345"),
    beskrivelse = beskrivelse,
)

fun lagreSanksjon(
    id: UUID? = null,
    sakId: SakId = STANDARDSAK,
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
