package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.regler.AvkortetYtelseGrunnlag
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.avkorteYtelse
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
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
import java.time.YearMonth

object InntektAvkortingService {

    private val logger = LoggerFactory.getLogger(InntektAvkortingService::class.java)

    fun beregnInntektsavkorting(avkortingGrunnlag: AvkortingGrunnlag): List<Avkortingsperiode> {
        logger.info("Beregner inntektsavkorting")
        val grunnlag = InntektAvkortingGrunnlag(
            inntekt = FaktumNode(verdi = avkortingGrunnlag.aarsinntekt, avkortingGrunnlag.kilde, "Forventet årsinntekt")
        )
        val resultat = kroneavrundetInntektAvkorting.eksekver(
            KonstantGrunnlag(grunnlag),
            RegelPeriode(avkortingGrunnlag.periode.fom.atDay(1))
        )
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
        beregninger: List<Beregningsperiode>,
        avkortingsperioder: List<Avkortingsperiode>
    ): List<AvkortetYtelse> {
        val regelgrunnlag = periodisertBruttoYtelseOgAvkorting(beregninger, avkortingsperioder)
        return regelgrunnlag.map { grunnlag ->
            val resultat = avkorteYtelse.eksekver(KonstantGrunnlag(grunnlag), grunnlag.periode)
            when (resultat) {
                is RegelkjoeringResultat.Suksess -> {
                    val tidspunkt = Tidspunkt.now()
                    resultat.periodiserteResultater.map { periodisertResultat ->
                        AvkortetYtelse(
                            periode = Periode(
                                fom = YearMonth.from(periodisertResultat.periode.fraDato),
                                tom = periodisertResultat.periode.tilDato?.let { YearMonth.from(it) }
                            ),
                            ytelseEtterAvkorting = periodisertResultat.resultat.verdi,
                            avkortingsbeloep = grunnlag.avkorting.verdi,
                            ytelseFoerAvkorting = grunnlag.bruttoYtelse.verdi,
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
        }.flatten()
    }

    private fun periodisertBruttoYtelseOgAvkorting(
        beregningsperioder: List<Beregningsperiode>,
        avkortingsperioder: List<Avkortingsperiode>
    ): List<AvkortetYtelseGrunnlag> {
        val knekkpunkter = beregningsperioder.map { it.datoFOM }.union(
            avkortingsperioder.map { it.periode.fom }
        ).sorted()

        val avkortetYtelseGrunnlag = mutableListOf<AvkortetYtelseGrunnlag>()
        knekkpunkter.forEachIndexed { indeks, knekkpunkt ->
            val beregning = beregningsperioder.filter { it.datoFOM <= knekkpunkt }.maxByOrNull { it.datoFOM }
                ?: throw Exception("Noe gikk galt ved uthenting av grunnlag for avkorting")

            val beregnetAvkorting = avkortingsperioder.filter { it.periode.fom <= knekkpunkt }
                .maxByOrNull { it.periode.fom }
                ?: throw Exception("Noe gikk galt ved uthenting av grunnlag for avkorting")

            avkortetYtelseGrunnlag.add(
                AvkortetYtelseGrunnlag(
                    periode = RegelPeriode(
                        fraDato = knekkpunkt.atDay(1),
                        tilDato = knekkpunkter.getOrNull(indeks + 1)?.minusMonths(1)?.atEndOfMonth()
                    ),
                    bruttoYtelse = FaktumNode(
                        verdi = beregning.utbetaltBeloep,
                        kilde = beregning.kilde ?: "",
                        beskrivelse = "Beregnet brutto ytelse for periode"
                    ),
                    avkorting = FaktumNode(
                        verdi = beregnetAvkorting.avkorting,
                        kilde = beregnetAvkorting.kilde,
                        beskrivelse = "Beregnet avkorting for periode"
                    )
                )
            )
        }
        return avkortetYtelseGrunnlag
    }
}