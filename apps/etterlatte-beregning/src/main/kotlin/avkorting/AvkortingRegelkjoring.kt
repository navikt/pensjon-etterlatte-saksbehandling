package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.regler.FaktiskInntektGrunnlag
import no.nav.etterlatte.avkorting.regler.ForventetInntektGrunnlag
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.MaanedInnvilget
import no.nav.etterlatte.avkorting.regler.PeriodisertAvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertInntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.RestanseGrunnlag
import no.nav.etterlatte.avkorting.regler.avkortetYtelseMedRestanseOgSanksjon
import no.nav.etterlatte.avkorting.regler.faktiskInntektInnvilgetPeriode
import no.nav.etterlatte.avkorting.regler.forventetInntektInnvilgetPeriode
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
import no.nav.etterlatte.avkorting.regler.restanse
import no.nav.etterlatte.avkorting.regler.restanse_v2
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodiseringAvGrunnlagFeil
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.regler.Beregningstall
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object AvkortingRegelkjoring {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun beregnInntektInnvilgetPeriodeForventetInntekt(
        inntektTom: Int,
        fratrekkInnAar: Int,
        inntektUtlandTom: Int,
        fratrekkInnAarUtland: Int,
        kilde: Grunnlagsopplysning.Saksbehandler,
        periode: Periode,
    ): BenyttetInntektInnvilgetPeriode {
        logger.info("Beregner forventet inntekt innvilget periode")

        val resultat =
            forventetInntektInnvilgetPeriode.eksekver(
                grunnlag =
                    KonstantGrunnlag(
                        FaktumNode(
                            ForventetInntektGrunnlag(
                                inntektTom = Beregningstall(inntektTom),
                                fratrekkInnAar = Beregningstall(fratrekkInnAar),
                                inntektUtlandTom = Beregningstall(inntektUtlandTom),
                                fratrekkInnAarUtland = Beregningstall(fratrekkInnAarUtland),
                            ),
                            kilde = kilde,
                            beskrivelse = "Forventet inntekt frem til opphør og før innvilgelse",
                        ),
                    ),
                periode = periode.tilRegelPeriode(),
            )
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                resultat.periodiserteResultater
                    .map { periodisertResultat ->
                        BenyttetInntektInnvilgetPeriode(
                            verdi = periodisertResultat.resultat.verdi.toInteger(),
                            tidspunkt = tidspunkt,
                            regelResultat = periodisertResultat.toJsonNode(),
                            kilde =
                                Grunnlagsopplysning.RegelKilde(
                                    navn = forventetInntektInnvilgetPeriode.regelReferanse.id,
                                    ts = tidspunkt,
                                    versjon = periodisertResultat.reglerVersjon,
                                ),
                        )
                    }.single()
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw InternfeilException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    fun beregnInntektInnvilgetPeriodeFaktiskInntekt(
        loennsinntekt: Int,
        afp: Int,
        naeringsinntekt: Int,
        utland: Int,
        periode: RegelPeriode,
        kilde: Grunnlagsopplysning.Saksbehandler,
    ): BenyttetInntektInnvilgetPeriode {
        logger.info("Beregner faktisk inntekt innvilget periode")

        val resultat =
            faktiskInntektInnvilgetPeriode.eksekver(
                grunnlag =
                    KonstantGrunnlag(
                        FaktumNode(
                            FaktiskInntektGrunnlag(
                                loennsinntekt = Beregningstall(loennsinntekt),
                                afp = Beregningstall(afp),
                                naeringsinntekt = Beregningstall(naeringsinntekt),
                                utland = Beregningstall(utland),
                            ),
                            kilde = kilde,
                            beskrivelse = "Faktisk inntekt for innvilget periode",
                        ),
                    ),
                periode = periode,
            )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                resultat.periodiserteResultater
                    .map { periodisertResultat ->
                        BenyttetInntektInnvilgetPeriode(
                            verdi = periodisertResultat.resultat.verdi.toInteger(),
                            tidspunkt = tidspunkt,
                            regelResultat = periodisertResultat.toJsonNode(),
                            kilde =
                                Grunnlagsopplysning.RegelKilde(
                                    navn = faktiskInntektInnvilgetPeriode.regelReferanse.id,
                                    ts = tidspunkt,
                                    versjon = faktiskInntektInnvilgetPeriode.regelReferanse.versjon,
                                ),
                        )
                    }.single()
            }

            is RegelkjoeringResultat.UgyldigPeriode -> throw InternfeilException("Ugyldig periode for regel for faktisk inntekt: $periode")
        }
    }

    fun beregnInntektsavkorting(
        periode: Periode,
        avkortingGrunnlag: AvkortingGrunnlag,
    ): List<Avkortingsperiode> {
        logger.info("Beregner inntektsavkorting")

        val inntektInnvilgetPeriode =
            avkortingGrunnlag.inntektInnvilgetPeriode.let {
                when (it) {
                    is BenyttetInntektInnvilgetPeriode -> it.verdi
                    is IngenInntektInnvilgetPeriode -> throw InternfeilException(
                        "Kan ikke beregne avkorting uten inntekt innvilget periode",
                    )
                }
            }
        val grunnlag =
            PeriodisertInntektAvkortingGrunnlag(
                periodisertInntektAvkortingGrunnlag =
                    PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                        listOf(avkortingGrunnlag)
                            .map {
                                GrunnlagMedPeriode(
                                    data = it,
                                    fom = periode.fom.atDay(1),
                                    tom = periode.tom?.atEndOfMonth(),
                                )
                            }.mapVerdier {
                                FaktumNode(
                                    verdi =
                                        InntektAvkortingGrunnlag(
                                            inntektInnvilgetNedrundet =
                                                Beregningstall(inntektInnvilgetPeriode),
                                            relevanteMaaneder = Beregningstall(avkortingGrunnlag.innvilgaMaaneder),
                                            grunnlagId = avkortingGrunnlag.id,
                                        ),
                                    kilde = avkortingGrunnlag.kilde,
                                    beskrivelse = "Forventet årsinntekt",
                                )
                            },
                    ) { datoIPeriode: LocalDate, foersteFom: LocalDate, senesteTom: LocalDate? ->
                        throw InternfeilException(
                            "Noe gikk galt ved uthenting av periodiserte avkortingsgrunnlag under beregning av inntektsavkorting. Dato i periode: $datoIPeriode, første fom: $foersteFom, senesteTom: $senesteTom",
                        )
                    },
            )

        val resultat = kroneavrundetInntektAvkorting.eksekver(grunnlag, periode.tilRegelPeriode())
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                resultat.periodiserteResultater.map { periodisertResultat ->

                    Avkortingsperiode(
                        id = UUID.randomUUID(),
                        periode =
                            Periode(
                                fom = YearMonth.from(periodisertResultat.periode.fraDato),
                                tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                            ),
                        avkorting = periodisertResultat.resultat.verdi,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde =
                            Grunnlagsopplysning.RegelKilde(
                                navn = kroneavrundetInntektAvkorting.regelReferanse.id,
                                ts = tidspunkt,
                                versjon = periodisertResultat.reglerVersjon,
                            ),
                        inntektsgrunnlag =
                            grunnlag
                                .finnGrunnlagForPeriode(
                                    periodisertResultat.periode.fraDato,
                                ).inntektAvkortingGrunnlag.verdi.grunnlagId,
                    )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw InternfeilException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    fun beregnAvkortetYtelse(
        periode: Periode,
        ytelseFoerAvkorting: List<YtelseFoerAvkorting>,
        avkortingsperioder: List<Avkortingsperiode>,
        type: AvkortetYtelseType,
        sanksjoner: List<Sanksjon>,
        restanse: Restanse? = null,
    ): List<AvkortetYtelse> {
        if (sanksjoner.isNotEmpty() && type == AvkortetYtelseType.FORVENTET_INNTEKT) {
            throw InternfeilException("Skal ikke regne med sanksjoner i avkorting av forventet inntekt")
        }

        val sanksjonsperioder =
            try {
                periodiserteSanksjoner(sanksjoner)
            } catch (e: PeriodiseringAvGrunnlagFeil) {
                when (e) {
                    is PeriodiseringAvGrunnlagFeil.PerioderOverlapper -> throw UgyldigForespoerselException(
                        code = "OVERLAPPENDE_SANKSJONER",
                        detail =
                            "Behandlingen har sanksjoner som overlapper med hverandre i perioder. Dobbelt " +
                                "sanksjon i en måned støttes ikke, og de overlappende sanksjonene må endres / " +
                                "fjernes for å kunne beregne avkortet ytelse.",
                        cause = e,
                    )

                    else -> throw InternfeilException(
                        "Kunne ikke sette opp perioder for sanksjon riktig. " +
                            "Feilen som oppstod var: ${e.code}",
                        e,
                    )
                }
            }

        val regelgrunnlag =
            PeriodisertAvkortetYtelseGrunnlag(
                beregningsperioder = periodiserteBeregninger(ytelseFoerAvkorting),
                avkortingsperioder = periodiserteAvkortinger(avkortingsperioder),
                fordeltRestanse = restansegrunnlag(restanse),
                sanksjonsperioder = sanksjonsperioder,
            )
        val resultat = avkortetYtelseMedRestanseOgSanksjon.eksekver(regelgrunnlag, periode.tilRegelPeriode())
        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                return resultat.periodiserteResultater.map { periodisertResultat ->
                    val resultatFom = periodisertResultat.periode.fraDato
                    val avkortingsbeloep = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).avkorting.verdi
                    val ytelseFoerAvkortingGrunnlag = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).beregning.verdi

                    val sanksjonForPeriode = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).sanksjon.verdi
                    AvkortetYtelse(
                        id = UUID.randomUUID(),
                        type = type,
                        periode =
                            Periode(
                                fom = YearMonth.from(periodisertResultat.periode.fraDato),
                                tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) },
                            ),
                        ytelseEtterAvkorting = periodisertResultat.resultat.verdi,
                        restanse = restanse,
                        sanksjon =
                            sanksjonForPeriode?.let {
                                SanksjonertYtelse(
                                    it.id!!,
                                    it.type,
                                )
                            },
                        ytelseEtterAvkortingFoerRestanse = ytelseFoerAvkortingGrunnlag - avkortingsbeloep,
                        avkortingsbeloep = avkortingsbeloep,
                        ytelseFoerAvkorting = ytelseFoerAvkortingGrunnlag,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde =
                            Grunnlagsopplysning.RegelKilde(
                                navn = kroneavrundetInntektAvkorting.regelReferanse.id,
                                ts = tidspunkt,
                                versjon = periodisertResultat.reglerVersjon,
                            ),
                    )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw InternfeilException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun periodiserteSanksjoner(sanksjonsperioder: List<Sanksjon>): PeriodisertGrunnlag<FaktumNode<Sanksjon?>> {
        if (sanksjonsperioder.isEmpty()) {
            return KonstantGrunnlag(
                FaktumNode(
                    null,
                    "Ingen sanksjoner innenfor årsoppgjør",
                    "Ingen sanksjoner innenfor årsoppgjør",
                ),
            )
        }

        return PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            sanksjonsperioder.map {
                GrunnlagMedPeriode(
                    data =
                        FaktumNode(
                            verdi = it,
                            beskrivelse = "Sanksjon: ${it.type}",
                            kilde = it.id!!,
                        ),
                    fom = it.fom.atDay(1),
                    tom = it.tom?.atEndOfMonth(),
                )
            },
        ) { _, _, _ -> FaktumNode(null, beskrivelse = "Ingen sanksjon i perioden", kilde = "Grunnlag") }
    }

    private fun periodiserteBeregninger(beregninger: List<YtelseFoerAvkorting>): PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            beregninger
                .map {
                    GrunnlagMedPeriode(
                        data = it,
                        fom = it.periode.fom.atDay(1),
                        tom = it.periode.tom?.atEndOfMonth(),
                    )
                }.mapVerdier {
                    FaktumNode(
                        verdi = it.beregning,
                        kilde = it.beregningsreferanse,
                        beskrivelse = "Beregnet ytelse før avkorting for periode",
                    )
                },
        ) { datoIPeriode: LocalDate, foersteFom: LocalDate, senesteTom: LocalDate? ->
            throw InternfeilException(
                "Noe gikk galt ved uthenting av periodiserte beregninger under beregning av avkortet ytelse. Dato i periode: $datoIPeriode, første fom: $foersteFom, senesteTom: $senesteTom",
            )
        }

    private fun periodiserteAvkortinger(avkortingGrunnlag: List<Avkortingsperiode>): PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            avkortingGrunnlag
                .map {
                    GrunnlagMedPeriode(
                        data = it,
                        fom = it.periode.fom.atDay(1),
                        tom = it.periode.tom?.atEndOfMonth(),
                    )
                }.mapVerdier {
                    FaktumNode(
                        verdi = it.avkorting,
                        kilde = "Avkorting:${it.id}",
                        beskrivelse = "Beregnet avkorting for periode",
                    )
                },
        ) { datoIPeriode: LocalDate, foersteFom: LocalDate, senesteTom: LocalDate? ->
            throw InternfeilException(
                "Noe gikk galt ved uthenting av periodiserte avkortinger under beregning av avkortet ytelse. Dato i periode: $datoIPeriode, første fom: $foersteFom, senesteTom: $senesteTom",
            )
        }

    private fun restansegrunnlag(restanse: Restanse?): KonstantGrunnlag<FaktumNode<Int>> =
        KonstantGrunnlag(
            FaktumNode(
                verdi = restanse?.fordeltRestanse ?: 0,
                kilde = restanse?.id?.let { "Restanse:$it" } ?: "",
                beskrivelse = "Restansebeløp som skal fordeles månedlig",
            ),
        )

    fun beregnRestanse(
        fraOgMed: YearMonth,
        nyInntektsavkorting: Inntektsavkorting,
        tidligereYtelseEtterAvkorting: List<AvkortetYtelse>,
        sanksjoner: List<Sanksjon>,
        brukNyeReglerAvkorting: Boolean,
    ): Restanse {
        val oppstartNyInntekt = nyInntektsavkorting.grunnlag.periode.fom
        val maanederMedSanksjonIAar =
            (1..12)
                .map {
                    YearMonth.of(fraOgMed.year, it)
                }.map { maaned ->
                    maaned to sanksjoner.any { it.fom <= maaned && (it.tom == null || it.tom!! >= maaned) }
                }
        val maanederInnvilget: FaktumNode<List<MaanedInnvilget>> =
            if (brukNyeReglerAvkorting) {
                FaktumNode(
                    (nyInntektsavkorting.grunnlag.maanederInnvilget ?: reparerEllerNoe()),
                    "",
                    "",
                )
            } else {
                FaktumNode(emptyList(), "", "")
            }

        val grunnlag =
            RestanseGrunnlag(
                tidligereYtelseEtterAvkorting =
                    FaktumNode(
                        verdi = tidligereYtelseEtterAvkorting.spreYtelsePerMaaned(fraOgMed, oppstartNyInntekt),
                        kilde = tidligereYtelseEtterAvkorting.map { "avkortetYtelse:${it.id}" },
                        beskrivelse = "Ytelse etter avkorting for tidligere oppgitt forventet årsinntekt samme år",
                    ),
                nyForventetYtelseEtterAvkorting =
                    FaktumNode(
                        verdi =
                            nyInntektsavkorting.avkortetYtelseForventetInntekt.spreYtelsePerMaaned(
                                fraOgMed,
                                oppstartNyInntekt,
                            ),
                        kilde = nyInntektsavkorting.grunnlag.id,
                        beskrivelse = "Ytelse etter avkorting med ny forventet årsinntekt",
                    ),
                fraOgMedNyForventetInntekt =
                    FaktumNode(
                        verdi = oppstartNyInntekt,
                        kilde = nyInntektsavkorting.grunnlag.id,
                        beskrivelse = "Tidspunkt ny forventet inntekt inntrer",
                    ),
                maanederOgSanksjon =
                    FaktumNode(
                        verdi = maanederMedSanksjonIAar,
                        kilde = sanksjoner.joinToString(prefix = "[", postfix = "]") { it.id.toString() },
                        beskrivelse = "Måneder i året og om det er en sanksjon for den måneden",
                    ),
                maanederInnvilget = maanederInnvilget,
            )

        val resultat =
            if (brukNyeReglerAvkorting) {
                restanse_v2.eksekver(
                    grunnlag = KonstantGrunnlag(grunnlag),
                    periode = Periode(fom = fraOgMed, tom = null).tilRegelPeriode(),
                )
            } else {
                restanse.eksekver(
                    grunnlag = KonstantGrunnlag(konstantGrunnlag = grunnlag),
                    periode = Periode(fom = fraOgMed, tom = null).tilRegelPeriode(),
                )
            }
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val (totalRestanse, fordeltRestanse) =
                    resultat.periodiserteResultater
                        .single()
                        .resultat.verdi
                val tidspunkt = Tidspunkt.now()
                Restanse(
                    id = UUID.randomUUID(),
                    totalRestanse = totalRestanse,
                    fordeltRestanse = fordeltRestanse,
                    regelResultat = resultat.toJsonNode(),
                    tidspunkt = tidspunkt,
                    kilde =
                        Grunnlagsopplysning.RegelKilde(
                            navn = restanse.regelReferanse.id,
                            ts = tidspunkt,
                            versjon = resultat.reglerVersjon,
                        ),
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw InternfeilException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun reparerEllerNoe(): List<MaanedInnvilget> {
        TODO()
    }

    private fun List<AvkortetYtelse>.spreYtelsePerMaaned(
        fraOgMed: YearMonth,
        til: YearMonth,
    ): List<Int> {
        val perMaaned = mutableListOf<Int>()
        if (fraOgMed == til) return perMaaned

        for (maanednr in fraOgMed.monthValue..til.minusMonths(1).monthValue) {
            val maaned = YearMonth.of(til.year, maanednr)
            perMaaned.add(avkortetYtelseIMaaned(maaned).ytelseEtterAvkorting)
        }
        return perMaaned
    }

    private fun List<AvkortetYtelse>.avkortetYtelseIMaaned(maaned: YearMonth) =
        this.find {
            maaned >= it.periode.fom && (it.periode.tom == null || maaned <= it.periode.tom)
        } ?: throw InternfeilException("Maaned finnes ikke i avkortet ytelse sin periode")
}

fun Periode.tilRegelPeriode(): RegelPeriode =
    RegelPeriode(
        fom.atDay(1),
        tom?.atEndOfMonth(),
    )
