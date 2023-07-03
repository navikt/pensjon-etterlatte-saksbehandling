package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.regler.FordelRestanseGrunnlag
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertAvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertInntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertRestanseGrunnlag
import no.nav.etterlatte.avkorting.regler.avkortetYtelseMedRestanse
import no.nav.etterlatte.avkorting.regler.fordeltRestanse
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
import no.nav.etterlatte.avkorting.regler.restanse
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
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
import java.lang.IllegalArgumentException
import java.time.YearMonth

object AvkortingRegelkjoring {

    private val logger = LoggerFactory.getLogger(AvkortingRegelkjoring::class.java)

    fun beregnInntektsavkorting(
        periode: Periode,
        avkortingGrunnlag: List<AvkortingGrunnlag>
    ): List<Avkortingsperiode> {
        logger.info("Beregner inntektsavkorting")

        val grunnlag = PeriodisertInntektAvkortingGrunnlag(
            periodisertInntektAvkortingGrunnlag = PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                avkortingGrunnlag.map {
                    GrunnlagMedPeriode(
                        data = it,
                        fom = it.periode.fom.atDay(1),
                        tom = it.periode.tom?.atEndOfMonth()
                    )
                }.mapVerdier {
                    FaktumNode(
                        verdi = InntektAvkortingGrunnlag(
                            inntekt = Beregningstall(it.aarsinntekt),
                            fratrekkInnUt = Beregningstall(it.fratrekkInnAar),
                            relevanteMaaneder = Beregningstall(it.relevanteMaanederInnAar)
                        ),
                        kilde = it.kilde,
                        beskrivelse = "Forventet årsinntekt"
                    )
                }
            ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") }
        )

        val resultat = kroneavrundetInntektAvkorting.eksekver(grunnlag, periode.tilRegelPeriode())
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                resultat.periodiserteResultater.map { periodisertResultat ->
                    Avkortingsperiode(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        avkorting = periodisertResultat.resultat.verdi,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            navn = kroneavrundetInntektAvkorting.regelReferanse.id,
                            ts = tidspunkt,
                            versjon = periodisertResultat.reglerVersjon
                        )
                    )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    // TODO egen fil..
    fun beregnRestanse(
        aarsoppgjoerMaaneder: List<AarsoppgjoerMaaned>
    ): Map<YearMonth, Restanse> {
        val periode = Periode(fom = aarsoppgjoerMaaneder.first().maaned, tom = aarsoppgjoerMaaneder.last().maaned)
        val grunnlag = PeriodisertRestanseGrunnlag(
            tidligereYtelseEtterAvkorting = PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                aarsoppgjoerMaaneder.map {
                    GrunnlagMedPeriode(
                        data = it.faktiskAvkortetYtelse,
                        fom = it.maaned.atDay(1),
                        tom = it.maaned.atEndOfMonth()
                    )
                }.mapVerdier {
                    FaktumNode(
                        verdi = it,
                        kilde = "", // TODO EY-2368
                        beskrivelse = "Forventet årsinntekt"
                    )
                }
            ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") },
            nyForventetYtelseEtterAvkorting = PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                aarsoppgjoerMaaneder.map {
                    GrunnlagMedPeriode(
                        data = it.forventetAvkortetYtelse,
                        fom = it.maaned.atDay(1),
                        tom = it.maaned.atEndOfMonth()
                    )
                }.mapVerdier {
                    FaktumNode(
                        verdi = it,
                        kilde = "", // TODO EY-2368
                        beskrivelse = "Forventet årsinntekt"
                    )
                }
            ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") }
        )

        val resultat = restanse.eksekver(grunnlag, periode.tilRegelPeriode())
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                resultat.periodiserteResultater.associate {
                    YearMonth.from(it.periode.fraDato) to
                            Restanse(
                                verdi = it.resultat.verdi,
                                regelResultat = it.toJsonNode(),
                                kilde = Grunnlagsopplysning.RegelKilde(
                                    navn = restanse.regelReferanse.id,
                                    ts = Tidspunkt.now(),
                                    versjon = it.reglerVersjon
                                )
                            )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    fun beregnFordeltRestanse(virkningstidspunkt: YearMonth, aarsoppgjoer: List<AarsoppgjoerMaaned>): FordeltRestanse {
        val regelgrunnlag = FordelRestanseGrunnlag(
            virkningstidspunkt = FaktumNode(
                verdi = virkningstidspunkt,
                kilde = "TODO", // TODO EY-2368 kilde - Bytt YearMonth med klasse for å kunne bruke kilde?
                beskrivelse = "Virkningstidspunkt hvor restanse skal fordeles månedlig fra"
            ),
            maanederMedRestanse = FaktumNode(
                verdi = aarsoppgjoer,
                kilde = "TODO", // TODO EY-2368 kilde - Hva blir kilde her id til tabell (enten nytt id felt eller ny tabell for å samle)
                beskrivelse = "All restanse til tidligere måneder i året"
            )
        )

        val resultat = fordeltRestanse.eksekver(
            KonstantGrunnlag(regelgrunnlag),
            RegelPeriode(fraDato = virkningstidspunkt.atDay(1))
        )
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                resultat.periodiserteResultater.map {
                    FordeltRestanse(
                        verdi = it.resultat.verdi,
                        regelResultat = it.toJsonNode(),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            navn = fordeltRestanse.regelReferanse.id,
                            ts = Tidspunkt.now(),
                            versjon = it.reglerVersjon
                        )
                    )
                }.first()
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    fun beregnAvkortetYtelsePaaNytt(aarsoppgjoerMaaneder: List<AarsoppgjoerMaaned>): List<AvkortetYtelse> {
        val periode = Periode(fom = aarsoppgjoerMaaneder.first().maaned, tom = aarsoppgjoerMaaneder.last().maaned)
        val avkortetYtelseGrunnlag = PeriodisertAvkortetYtelseGrunnlag(
            beregningsperioder = maanedligeBeregninger(aarsoppgjoerMaaneder),
            avkortingsperioder = maanedligeAvkortinger(aarsoppgjoerMaaneder),
            fordeltRestanse = KonstantGrunnlag(FaktumNode(verdi = 0, kilde = "", beskrivelse = "Tom restanse"))
        )
        return beregnAvkortetYtelse(periode, avkortetYtelseGrunnlag)
    }

    fun beregnAvkortetYtelse(
        periode: Periode,
        beregningsperioder: List<Beregningsperiode>,
        avkortingsperioder: List<Avkortingsperiode>,
        maanedligRestanse: FordeltRestanse? = null
    ): List<AvkortetYtelse> {
        val regelgrunnlag = PeriodisertAvkortetYtelseGrunnlag(
            beregningsperioder = periodiserteBeregninger(beregningsperioder),
            avkortingsperioder = periodiserteAvkortinger(avkortingsperioder),
            fordeltRestanse = KonstantGrunnlag(
                FaktumNode(
                    verdi = maanedligRestanse?.verdi ?: 0,
                    kilde = maanedligRestanse?.kilde ?: "", // TODO EY-2368 kilde
                    beskrivelse = "Restansebeløp som skal fordeles månedlig"
                )
            )
        )
        return beregnAvkortetYtelse(periode, regelgrunnlag)
    }

    private fun beregnAvkortetYtelse(
        periode: Periode,
        regelgrunnlag: PeriodisertAvkortetYtelseGrunnlag
    ): List<AvkortetYtelse> {
        val resultat = avkortetYtelseMedRestanse.eksekver(regelgrunnlag, periode.tilRegelPeriode())
        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                return resultat.periodiserteResultater.map { periodisertResultat ->
                    val resultatFom = periodisertResultat.periode.fraDato
                    val restanse = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).fordeltRestanse.verdi
                    AvkortetYtelse(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        ytelseEtterAvkorting = periodisertResultat.resultat.verdi,
                        restanse = restanse,
                        ytelseEtterAvkortingFoerRestanse = periodisertResultat.resultat.verdi + restanse,
                        avkortingsbeloep = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).avkorting.verdi,
                        ytelseFoerAvkorting = regelgrunnlag.finnGrunnlagForPeriode(resultatFom).beregning.verdi,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            navn = kroneavrundetInntektAvkorting.regelReferanse.id,
                            ts = tidspunkt,
                            versjon = periodisertResultat.reglerVersjon
                        )
                    )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun maanedligeBeregninger(
        aarsoppgjoerMaaneder: List<AarsoppgjoerMaaned>
    ): PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            aarsoppgjoerMaaneder.map {
                GrunnlagMedPeriode(
                    data = it,
                    fom = it.maaned.atDay(1),
                    tom = it.maaned.atEndOfMonth()
                )
            }.mapVerdier {
                FaktumNode(
                    verdi = it.beregning,
                    kilde = "", // TODO EY-2368 kilde
                    beskrivelse = "Beregnet ytelse før avkorting i måned"
                )
            }
        ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") }

    private fun periodiserteBeregninger(beregninger: List<Beregningsperiode>): PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            beregninger.map {
                GrunnlagMedPeriode(
                    data = it,
                    fom = it.datoFOM.atDay(1),
                    tom = it.datoTOM?.atEndOfMonth()
                )
            }.mapVerdier {
                FaktumNode(
                    verdi = it.utbetaltBeloep,
                    kilde = it.kilde
                        ?: throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger"),
                    beskrivelse = "Beregnet ytelse før avkorting for periode"
                )
            }
        ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") }

    private fun maanedligeAvkortinger(
        aarsoppgjoerMaaneder: List<AarsoppgjoerMaaned>
    ): PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            aarsoppgjoerMaaneder.map {
                GrunnlagMedPeriode(
                    data = it,
                    fom = it.maaned.atDay(1),
                    tom = it.maaned.atEndOfMonth()
                )
            }.mapVerdier {
                FaktumNode(
                    verdi = it.avkorting,
                    kilde = "", // TODO EY-2368 kilde
                    beskrivelse = "Beregnet avkorting i måned"
                )
            }
        ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte avkortinger") }

    private fun periodiserteAvkortinger(avkortingGrunnlag: List<Avkortingsperiode>):
            PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            avkortingGrunnlag.map {
                GrunnlagMedPeriode(
                    data = it,
                    fom = it.periode.fom.atDay(1),
                    tom = it.periode.tom?.atEndOfMonth()
                )
            }.mapVerdier {
                FaktumNode(
                    verdi = it.avkorting,
                    kilde = it.kilde,
                    beskrivelse = "Beregnet avkorting for periode"
                )
            }
        ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte avkortinger") }
}

fun Periode.tilRegelPeriode(): RegelPeriode = RegelPeriode(
    fom.atDay(1),
    tom?.atEndOfMonth()
)