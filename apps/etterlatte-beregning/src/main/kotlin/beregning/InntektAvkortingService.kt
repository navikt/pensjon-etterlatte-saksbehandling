package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.PeriodisertBeregningGrunnlag
import no.nav.etterlatte.beregning.grunnlag.mapVerdier
import no.nav.etterlatte.beregning.regler.avkorting.InntektAvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.PeriodisertAvkortetYtelseGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.avkorteYtelse
import no.nav.etterlatte.beregning.regler.avkorting.inntektAvkorting
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.time.YearMonth

object InntektAvkortingService {

    private val logger = LoggerFactory.getLogger(InntektAvkortingService::class.java)

    fun beregnInntektsavkorting(avkortingGrunnlag: AvkortingGrunnlag): List<BeregnetAvkortingGrunnlag> {
        logger.info("Beregner inntektsavkorting")
        val grunnlag = InntektAvkortingGrunnlag(
            inntekt = FaktumNode(verdi = avkortingGrunnlag.aarsinntekt, avkortingGrunnlag.kilde, "Forventet årsinntekt")
        )
        val resultat = inntektAvkorting.eksekver(
            KonstantGrunnlag(grunnlag),
            RegelPeriode(avkortingGrunnlag.periode.fom.atDay(1))
        )
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                resultat.periodiserteResultater.map { periodisertResultat ->
                    BeregnetAvkortingGrunnlag(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        avkorting = periodisertResultat.resultat.verdi,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            navn = inntektAvkorting.regelReferanse.id,
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
        beregninger: List<Beregningsperiode>,
        avkortingGrunnlag: List<AvkortingGrunnlag>,
        periode: RegelPeriode
    ): List<AvkortetYtelse> {
        val regelgrunnlag = periodisertBruttoYtelseOgAvkorting(beregninger, avkortingGrunnlag)
        val resultat = avkorteYtelse.eksekver(regelgrunnlag, periode)
        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val tidspunkt = Tidspunkt.now()
                return resultat.periodiserteResultater.map { periodisertResultat ->
                    AvkortetYtelse(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        ytelseEtterAvkorting = periodisertResultat.resultat.verdi,
                        tidspunkt = tidspunkt,
                        regelResultat = periodisertResultat.toJsonNode(),
                        kilde = Grunnlagsopplysning.RegelKilde(
                            navn = inntektAvkorting.regelReferanse.id,
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

    private fun periodisertBruttoYtelseOgAvkorting(
        beregninger: List<Beregningsperiode>,
        avkortingGrunnlag: List<AvkortingGrunnlag>
    ): PeriodisertAvkortetYtelseGrunnlag {
        return PeriodisertAvkortetYtelseGrunnlag(
            bruttoYtelse = PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                beregninger.tilPeriodisertBeregning().mapVerdier {
                    FaktumNode(
                        verdi = it.utbetaltBeloep,
                        kilde = it.kilde ?: "",
                        beskrivelse = "Beregnet brutto ytelse for periode"
                    )
                }
            ) { _, _, _ -> throw IllegalArgumentException() },
            avkorting = PeriodisertBeregningGrunnlag.lagGrunnlagMedDefaultUtenforPerioder(
                avkortingGrunnlag
                    .flatMap { it.beregnetAvkorting }
                    .tilPeriodisertGrunnlag().mapVerdier {
                        FaktumNode(
                            verdi = it.avkorting,
                            kilde = it.kilde,
                            beskrivelse = "Beregnet avkorting for periode"
                        )
                    }
            ) { _, _, _ -> throw IllegalArgumentException() }
        )
    }

    fun List<Beregningsperiode>.tilPeriodisertBeregning(): List<GrunnlagMedPeriode<Beregningsperiode>> {
        return this.map {
            GrunnlagMedPeriode(
                data = it,
                fom = it.datoFOM.atDay(1),
                tom = it.datoTOM?.atEndOfMonth()
            )
        }
    }

    fun List<BeregnetAvkortingGrunnlag>.tilPeriodisertGrunnlag(): List<GrunnlagMedPeriode<BeregnetAvkortingGrunnlag>> {
        return this.map {
            GrunnlagMedPeriode(
                data = it,
                fom = it.periode.fom.atDay(1),
                tom = it.periode.tom?.atEndOfMonth()
            )
        }
    }
}