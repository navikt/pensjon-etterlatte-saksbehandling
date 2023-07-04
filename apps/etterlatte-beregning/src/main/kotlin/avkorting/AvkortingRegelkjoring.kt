package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertAvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertInntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.RestanseGrunnlag
import no.nav.etterlatte.avkorting.regler.avkortetYtelseMedRestanse
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
import no.nav.etterlatte.avkorting.regler.restanse
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
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

    fun beregnAvkortetYtelse(
        virkningstidspunkt: YearMonth,
        beregningsperioder: List<YtelseFoerAvkorting>,
        avkortingsperioder: List<Avkortingsperiode>,
        restanse: Restanse? = null
    ): List<AvkortetYtelse> {
        val periode = Periode(fom = virkningstidspunkt, tom = null)
        val regelgrunnlag = PeriodisertAvkortetYtelseGrunnlag(
            beregningsperioder = periodiserteBeregninger(beregningsperioder),
            avkortingsperioder = periodiserteAvkortinger(avkortingsperioder),
            fordeltRestanse = KonstantGrunnlag(
                FaktumNode(
                    verdi = restanse?.fordeltRestanse ?: 0,
                    kilde = restanse?.kilde?: "", // TODO EY-2368 kilde
                    beskrivelse = "Restansebeløp som skal fordeles månedlig"
                )
            )
        )
        return beregnAvkortetYtelse(periode, regelgrunnlag)
    }

    fun beregnAvkortetYtelsePaaNytt(
        virkningstidspunkt: YearMonth,
        beregninger: List<YtelseFoerAvkorting>,
        avkortinger: List<Avkortingsperiode>
    ): List<AvkortetYtelse> {
        val periode = Periode(fom = beregninger.first().periode.fom, tom = virkningstidspunkt.minusMonths(1))
        val avkortetYtelseGrunnlag = PeriodisertAvkortetYtelseGrunnlag(
            beregningsperioder = periodiserteBeregninger(beregninger),
            avkortingsperioder = periodiserteAvkortinger(avkortinger),
            fordeltRestanse = KonstantGrunnlag(FaktumNode(verdi = 0, kilde = "", beskrivelse = "Tom restanse"))
        )
        return beregnAvkortetYtelse(periode, avkortetYtelseGrunnlag)
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

    private fun periodiserteBeregninger(beregninger: List<YtelseFoerAvkorting>): PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            beregninger.map {
                GrunnlagMedPeriode(
                    data = it,
                    fom = it.periode.fom.atDay(1),
                    tom = it.periode.tom?.atEndOfMonth()
                )
            }.mapVerdier {
                FaktumNode(
                    verdi = it.beregning,
                    kilde = it.beregningsreferanse,
                    beskrivelse = "Beregnet ytelse før avkorting for periode"
                )
            }
        ) { _, _, _ -> throw IllegalArgumentException("Noe gikk galt ved uthenting av periodiserte beregninger") }

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

    fun beregnRestanse(
        foersteMaaned: YearMonth,
        virkningstidspunkt: YearMonth,
        tidligereYtelseEtterAvkorting: List<AvkortetYtelse>,
        nyYtelseEtterAvkorting: List<AvkortetYtelse>
    ): Restanse {

        val grunnlag = RestanseGrunnlag(
            FaktumNode(
                verdi = tidligereYtelseEtterAvkorting
                    .sprePerMaaned(foersteMaaned, virkningstidspunkt)
                    .map { it.ytelseEtterAvkorting },
                kilde = tidligereYtelseEtterAvkorting.map { it.kilde }, // TODO EY-2361 liste med ider
                beskrivelse = "TODO" // TODO EY-2361
            ),
            FaktumNode(
                verdi = nyYtelseEtterAvkorting
                    .sprePerMaaned(foersteMaaned, virkningstidspunkt)
                    .map { it.ytelseEtterAvkorting },
                kilde = nyYtelseEtterAvkorting.map { it.kilde }, // TODO EY-2361
                beskrivelse = "TODO" // TODO EY-2361
            ),
            virkningstidspunkt = FaktumNode(
                verdi = virkningstidspunkt,
                kilde = "TODO", // TODO EY-2368 kilde - Bytt YearMonth med klasse for å kunne bruke kilde?
                beskrivelse = "Virkningstidspunkt hvor restanse skal fordeles månedlig fra"
            )
        )

        val resultat = restanse.eksekver(
            KonstantGrunnlag(grunnlag),
            Periode(fom = foersteMaaned, tom = null).tilRegelPeriode()
        )
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val restanseresultat = resultat.periodiserteResultater.first().resultat.verdi
                Restanse(
                    totalRestanse = restanseresultat.totalRestanse,
                    fordeltRestanse = restanseresultat.fordeltRestanse,
                    regelResultat = resultat.toJsonNode(),
                    kilde = Grunnlagsopplysning.RegelKilde(
                        navn = restanse.regelReferanse.id,
                        ts = Tidspunkt.now(),
                        versjon = resultat.reglerVersjon,
                    )
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    private fun List<AvkortetYtelse>.sprePerMaaned(
        foersteMaaned: YearMonth,
        virkningstidspunkt: YearMonth
    ): List<AvkortetYtelse> {
        val perMaaned = mutableListOf<AvkortetYtelse>()
        for (maanednr in foersteMaaned.monthValue ..virkningstidspunkt.minusMonths(1).monthValue) {
            val maaned = YearMonth.of(virkningstidspunkt.year, maanednr)
            perMaaned.add(avkortetYtelseIMaaned(maaned))
        }
        return perMaaned
    }

    private fun List<AvkortetYtelse>.avkortetYtelseIMaaned(maaned: YearMonth) = this.find {
        maaned >= it.periode.fom && (it.periode.tom == null || maaned <= it.periode.tom)
    } ?: throw Exception("Maaned finnes ikke i avkortet ytelse sin periode")

}

fun Periode.tilRegelPeriode(): RegelPeriode = RegelPeriode(
    fom.atDay(1),
    tom?.atEndOfMonth()
)