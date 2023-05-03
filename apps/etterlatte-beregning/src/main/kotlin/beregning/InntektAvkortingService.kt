package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.regler.avkorting.InntektAvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.avkorting.inntektAvkorting
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

object InntektAvkortingService {

    private val logger = LoggerFactory.getLogger(InntektAvkortingService::class.java)

    fun beregnInntektsavkorting(avkortingGrunnlag: List<AvkortingGrunnlag>): List<BeregnetAvkortingGrunnlag> {
        logger.info("Beregner inntektsavkorting")
        return avkortingGrunnlag.map {
            val grunnlag = InntektAvkortingGrunnlag(
                inntekt = FaktumNode(verdi = it.aarsinntekt, "TODO kilde", "Forventet årsinntekt")
            )
            val resultat = inntektAvkorting.eksekver(grunnlag, RegelPeriode(LocalDate.now()))
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
    }
}