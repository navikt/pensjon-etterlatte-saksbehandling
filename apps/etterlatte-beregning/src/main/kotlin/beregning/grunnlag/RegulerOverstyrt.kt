package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.beregning.regler.overstyr.OverstyrGrunnlag
import no.nav.etterlatte.beregning.regler.overstyr.RegulerManuellBeregningGrunnlag
import no.nav.etterlatte.beregning.regler.overstyr.grunnbeloep
import no.nav.etterlatte.beregning.regler.overstyr.regulerOverstyrt
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.regler.Beregningstall
import java.time.YearMonth

fun regulerOverstyrtBeregningsgrunnlag(
    reguleringsmaaned: YearMonth,
    forrigeGrunnlagBeloep: Long,
): Int {
    val (forrigeGrunnbeloep, nyttGrunnbeloep) = utledGrunbeloep(reguleringsmaaned)

    val resultat =
        regulerOverstyrt.eksekver(
            grunnlag =
                KonstantGrunnlag(
                    RegulerManuellBeregningGrunnlag(
                        manueltBeregnetBeloep =
                            FaktumNode(
                                verdi = Beregningstall(forrigeGrunnlagBeloep.toInt()),
                                "",
                                "",
                            ),
                        // TODO full G eller månedlig G?
                        forrigeGrunnbeloep =
                            FaktumNode(
                                verdi = Beregningstall(forrigeGrunnbeloep.grunnbeloepPerMaaned),
                                "",
                                "",
                            ),
                        nyttGrunnbeloep =
                            FaktumNode(
                                verdi = Beregningstall(nyttGrunnbeloep.grunnbeloepPerMaaned),
                                "",
                                "",
                            ),
                    ),
                ),
            periode =
                RegelPeriode(
                    fraDato = reguleringsmaaned.minusMonths(1).atDay(1),
                    tilDato = reguleringsmaaned.atEndOfMonth(),
                ),
        )
    return when (resultat) {
        is RegelkjoeringResultat.Suksess -> {
            resultat.periodiserteResultater.single().resultat.verdi.toInteger()
        }
        is RegelkjoeringResultat.UgyldigPeriode ->
            throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
    }
}

private fun utledGrunbeloep(reguleringsmaaned: YearMonth) =
    grunnbeloep.eksekver(
        // TODO lage en regel som henter grunnbeloep med "tomt" grunnlag?
        grunnlag =
            KonstantGrunnlag(
                OverstyrGrunnlag(
                    FaktumNode(
                        verdi =
                            OverstyrBeregningGrunnlagData(
                                utbetaltBeloep = 0,
                                trygdetid = 0,
                                trygdetidForIdent = null,
                                prorataBroekTeller = null,
                                prorataBroekNevner = null,
                                beskrivelse = "",
                            ),
                        kilde = "",
                        beskrivelse = "",
                    ),
                ),
            ),
        periode =
            RegelPeriode(
                fraDato = reguleringsmaaned.minusMonths(1).atDay(1),
                tilDato = reguleringsmaaned.atEndOfMonth(),
            ),
    ).let { resultat ->
        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                // assert(toSisteGrunnbeloep) TODO kun 2
                resultat.periodiserteResultater.let {
                    Pair(it[0].resultat.verdi, it[1].resultat.verdi)
                }
            }
            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }
