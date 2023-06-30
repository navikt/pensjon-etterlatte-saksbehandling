package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertAvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.PeriodisertInntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.avkorteYtelse
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
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

    fun beregnRestanse(aarsoppgjoerMaaned: AarsoppgjoerMaaned): AarsoppgjoerMaaned {
        val regelgrunnlag = PeriodisertAvkortetYtelseGrunnlag(
            beregningsperioder = periodiserteBeregning(aarsoppgjoerMaaned),
            avkortingsperioder = periodiserteAvkorting(aarsoppgjoerMaaned)
        )
        val nyForventetYtelseEtterAvkorting = beregnAvkortetYtelse(
            Periode(fom = aarsoppgjoerMaaned.maaned, tom = aarsoppgjoerMaaned.maaned),
            regelgrunnlag
        ).first()

        // TODO EY-2368 regel
        val beregnetRestanse =
            aarsoppgjoerMaaned.faktiskAvkortetYtelse - nyForventetYtelseEtterAvkorting.ytelseEtterAvkorting

        return aarsoppgjoerMaaned.copy(
            restanse = beregnetRestanse,
            forventetAvkortetYtelse = nyForventetYtelseEtterAvkorting.ytelseEtterAvkorting
        )
    }

    fun beregnFordeltRestanse(virkningstidspunkt: YearMonth, aarsoppgjoer: List<AarsoppgjoerMaaned>): Int {
        // TODO EY-2368 regel
        return aarsoppgjoer.sumOf { it.restanse } / (12 - virkningstidspunkt.monthValue + 1)
    }

    fun beregnAvkortetYtelse(
        periode: Periode,
        beregningsperioder: List<Beregningsperiode>,
        avkortingsperioder: List<Avkortingsperiode>,
        maanedligRestanse: Int = 0 // TODO EY-2368 - må gjøres om til grunnlag
    ): List<AvkortetYtelse> {
        val regelgrunnlag = PeriodisertAvkortetYtelseGrunnlag(
            beregningsperioder = periodiserteBeregninger(beregningsperioder),
            avkortingsperioder = periodiserteAvkortinger(avkortingsperioder)
        )
        return beregnAvkortetYtelse(periode, regelgrunnlag, maanedligRestanse)
    }

    private fun beregnAvkortetYtelse(
        periode: Periode,
        regelgrunnlag: PeriodisertAvkortetYtelseGrunnlag,
        maanedligRestanse: Int = 0 // TODO EY-2368 - må gjøres om til grunnlag
    ): List<AvkortetYtelse> {
        val resultat = avkorteYtelse.eksekver(regelgrunnlag, periode.tilRegelPeriode())
        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                return resultat.periodiserteResultater.map { periodisertResultat ->
                    val resultatFom = periodisertResultat.periode.fraDato
                    AvkortetYtelse(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        ytelseEtterAvkorting = periodisertResultat.resultat.verdi - maanedligRestanse, // TODO EY-2368
                        restanse = maanedligRestanse,
                        ytelseEtterAvkortingFoerRestanse = periodisertResultat.resultat.verdi,
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

    private fun periodiserteBeregning(aarsoppgjoerMaaned: AarsoppgjoerMaaned):
        PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            listOf(
                GrunnlagMedPeriode(
                    data = aarsoppgjoerMaaned,
                    fom = aarsoppgjoerMaaned.maaned.atDay(1),
                    tom = aarsoppgjoerMaaned.maaned.atEndOfMonth()
                )
            ).mapVerdier {
                FaktumNode(
                    verdi = it.beregning,
                    kilde = "", // TODO EY-2368
                    beskrivelse = ""
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

    private fun periodiserteAvkorting(aarsoppgjoerMaaned: AarsoppgjoerMaaned):
        PeriodisertGrunnlag<FaktumNode<Int>> =
        PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
            listOf(
                GrunnlagMedPeriode(
                    data = aarsoppgjoerMaaned,
                    fom = aarsoppgjoerMaaned.maaned.atDay(1),
                    tom = aarsoppgjoerMaaned.maaned.atEndOfMonth()
                )
            ).mapVerdier {
                FaktumNode(
                    verdi = it.avkorting,
                    kilde = "", // TODO EY-2368
                    beskrivelse = ""
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