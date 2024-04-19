package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.beregning.regler.overstyr.RegulerManuellBeregningGrunnlag
import no.nav.etterlatte.beregning.regler.overstyr.grunnbeloepUtenGrunnlag
import no.nav.etterlatte.beregning.regler.overstyr.regulerOverstyrtKroneavrundet
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
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
        regulerOverstyrtKroneavrundet.eksekver(
            grunnlag =
                KonstantGrunnlag(
                    RegulerManuellBeregningGrunnlag(
                        manueltBeregnetBeloep =
                            FaktumNode(
                                verdi = Beregningstall(forrigeGrunnlagBeloep.toInt()),
                                Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now()),
                                beskrivelse = "Forrige manuelt overstyrte beregning",
                            ),
                        forrigeGrunnbeloep =
                            FaktumNode(
                                verdi = Beregningstall(forrigeGrunnbeloep.grunnbeloepPerMaaned),
                                Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now()),
                                beskrivelse = "Forrige grunnbeløp brukt til å manuelt utregne beregning",
                            ),
                        nyttGrunnbeloep =
                            FaktumNode(
                                verdi = Beregningstall(nyttGrunnbeloep.grunnbeloepPerMaaned),
                                Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now()),
                                beskrivelse = "Nytt grunbeløp beregnins skal reguleres etter",
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
            resultat.periodiserteResultater.single().resultat.verdi
        }
        is RegelkjoeringResultat.UgyldigPeriode ->
            throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
    }
}

private fun utledGrunbeloep(reguleringsmaaned: YearMonth) =
    grunnbeloepUtenGrunnlag.eksekver(
        grunnlag = KonstantGrunnlag(""),
        periode =
            RegelPeriode(
                fraDato = reguleringsmaaned.minusYears(1).atDay(1),
                tilDato = reguleringsmaaned.atEndOfMonth(),
            ),
    ).let { resultat ->
        when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                assert(resultat.periodiserteResultater.size == 2)
                resultat.periodiserteResultater.let {
                    val gammelG: Grunnbeloep = it[0].resultat.verdi
                    assert(gammelG.dato == reguleringsmaaned.minusYears(1))
                    val nyG: Grunnbeloep = it[1].resultat.verdi
                    assert(nyG.dato == reguleringsmaaned)

                    Pair(gammelG, nyG)
                }
            }
            is RegelkjoeringResultat.UgyldigPeriode ->
                throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
        }
    }
