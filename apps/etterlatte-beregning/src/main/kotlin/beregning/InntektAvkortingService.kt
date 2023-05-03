package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.regler.avkorting.AvkortetYtelseGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.InntektAvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.avkorteYtelse
import no.nav.etterlatte.beregning.regler.avkorting.inntektAvkorting
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import org.slf4j.LoggerFactory
import java.time.YearMonth

object InntektAvkortingService {

    private val logger = LoggerFactory.getLogger(InntektAvkortingService::class.java)

    fun beregnInntektsavkorting(avkortingGrunnlag: AvkortingGrunnlag): List<BeregnetAvkortingGrunnlag> {
        logger.info("Beregner inntektsavkorting")
        val grunnlag = InntektAvkortingGrunnlag(
            inntekt = FaktumNode(verdi = avkortingGrunnlag.aarsinntekt, avkortingGrunnlag.kilde, "Forventet årsinntekt")
        )
        val resultat = inntektAvkorting.eksekver(grunnlag, RegelPeriode(avkortingGrunnlag.periode.fom.atDay(1)))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                resultat.periodiserteResultater.map { periodisertResultat ->
                    BeregnetAvkortingGrunnlag(
                        periode = Periode(
                            fom = YearMonth.from(periodisertResultat.periode.fraDato),
                            tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                        ),
                        avkorting = periodisertResultat.resultat.verdi,
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = periodisertResultat.toJsonNode()
                    )
                }
            }

            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }

    fun beregnAvkortetYtelse(
        beregninger: List<Beregningsperiode>,
        avkortingGrunnlag: List<AvkortingGrunnlag>
    ): List<AvkortetYtelse> {
        val avkortingsgrunnlag = periodisertBruttoYtelseOgAvkorting(beregninger, avkortingGrunnlag)
        return avkortingsgrunnlag.map { grunnlag ->
            val resultat = avkorteYtelse.eksekver(grunnlag, RegelPeriode(grunnlag.fom.atDay(1)))
            when (resultat) {
                is RegelkjoeringResultat.Suksess -> {
                    resultat.periodiserteResultater.map { periodisertResultat ->
                        AvkortetYtelse(
                            periode = Periode(
                                fom = YearMonth.from(periodisertResultat.periode.fraDato),
                                tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                            ),
                            ytelseEtterAvkorting = periodisertResultat.resultat.verdi,
                            tidspunkt = Tidspunkt.now(),
                            regelResultat = periodisertResultat.toJsonNode()
                        )
                    }
                }

                is RegelkjoeringResultat.UgyldigPeriode ->
                    throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
            }
        }.flatten()
    }

    private fun periodisertBruttoYtelseOgAvkorting(
        beregninger: List<Beregningsperiode>,
        avkortingGrunnlag: List<AvkortingGrunnlag>
    ): List<AvkortetYtelseGrunnlag> {
        val beregnedeAvkortinger = avkortingGrunnlag.flatMap { it.beregnetAvkorting }
        val knekkpunkter = beregninger.map { it.datoFOM }.toMutableSet() + beregnedeAvkortinger.map { it.periode.fom }

        return knekkpunkter.map { knekkpunkt ->
            val beregning = beregninger.filter { it.datoFOM <= knekkpunkt }.maxByOrNull { it.datoFOM }
                ?: throw Exception("Noe gikk galt ved uthenting av grunnlag for avkorting")

            val beregnetAvkorting = beregnedeAvkortinger.filter { it.periode.fom <= knekkpunkt }
                .maxByOrNull { it.periode.fom }
                ?: throw Exception("Noe gikk galt ved uthenting av grunnlag for avkorting")

            AvkortetYtelseGrunnlag(
                fom = knekkpunkt,
                bruttoYtelse = FaktumNode(
                    verdi = beregning.utbetaltBeloep,
                    kilde = "Regel", // TODO EY-2123
                    beskrivelse = "Beregnet brutto ytelse for periode"
                ),
                avkorting = FaktumNode(
                    verdi = beregnetAvkorting.avkorting,
                    kilde = "Regel", // TODO EY-2123
                    beskrivelse = "Beregnet avkorting for periode"
                )
            )
        }
    }
}