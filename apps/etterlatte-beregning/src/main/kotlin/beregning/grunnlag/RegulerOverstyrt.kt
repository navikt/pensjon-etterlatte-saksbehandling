package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.beregning.regler.overstyr.RegulerManuellBeregningGrunnlag
import no.nav.etterlatte.beregning.regler.overstyr.grunnbeloepUtenGrunnlag
import no.nav.etterlatte.beregning.regler.overstyr.regulerOverstyrtKroneavrundet
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.regler.Beregningstall
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun tilpassOverstyrtBeregningsgrunnlagForRegulering(
    reguleringsmaaned: YearMonth,
    fom: LocalDate,
    forrigeBeregningsgrunnlag: OverstyrBeregningGrunnlagDao,
    behandlingId: UUID,
): OverstyrBeregningGrunnlagDao {
    val (forrigeGrunnbeloep, nyttGrunnbeloep) = utledGrunnbeloep(reguleringsmaaned)
    val resultat =
        regulerOverstyrtKroneavrundet
            .eksekver(
                grunnlag =
                    KonstantGrunnlag(
                        RegulerManuellBeregningGrunnlag(
                            manueltBeregnetBeloep =
                                FaktumNode(
                                    verdi = Beregningstall(forrigeBeregningsgrunnlag.utbetaltBeloep.toInt()),
                                    Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now()),
                                    beskrivelse = "Forrige manuelt overstyrte beregning",
                                ),
                            forrigeGrunnbeloep =
                                FaktumNode(
                                    verdi = Beregningstall(forrigeGrunnbeloep.grunnbeloep),
                                    Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now()),
                                    beskrivelse = "Forrige grunnbeløp brukt til å manuelt utregne beregning",
                                ),
                            nyttGrunnbeloep =
                                FaktumNode(
                                    verdi = Beregningstall(nyttGrunnbeloep.grunnbeloep),
                                    Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now()),
                                    beskrivelse = "Nytt grunnbeløp beregnins skal reguleres etter",
                                ),
                        ),
                    ),
                periode =
                    RegelPeriode(
                        fraDato = fom,
                    ),
            ).let {
                when (it) {
                    is RegelkjoeringResultat.Suksess -> {
                        it.periodiserteResultater.single()
                    }
                    is RegelkjoeringResultat.UgyldigPeriode ->
                        throw RuntimeException("Ugyldig regler for periode: ${it.ugyldigeReglerForPeriode}")
                }
            }
    return forrigeBeregningsgrunnlag.copy(
        id = UUID.randomUUID(),
        behandlingId = behandlingId,
        datoFOM = fom,
        datoTOM = forrigeBeregningsgrunnlag.datoTOM,
        utbetaltBeloep = resultat.resultat.verdi.toLong(),
        kilde = Grunnlagsopplysning.automatiskSaksbehandler,
        reguleringRegelresultat = objectMapper.valueToTree(resultat),
        beskrivelse = forrigeBeregningsgrunnlag.beskrivelse,
    )
}

private fun utledGrunnbeloep(reguleringsmaaned: YearMonth) =
    grunnbeloepUtenGrunnlag
        .eksekver(
            grunnlag = KonstantGrunnlag(""),
            periode =
                RegelPeriode(
                    fraDato = reguleringsmaaned.minusMonths(1).atDay(1),
                    tilDato = reguleringsmaaned.atEndOfMonth(),
                ),
        ).let { resultat ->
            when (resultat) {
                is RegelkjoeringResultat.Suksess -> {
                    check(resultat.periodiserteResultater.size == 2) {
                        "Fikk uventet antall perioder for utleding av grunnlag: ${resultat.periodiserteResultater.size}"
                    }
                    resultat.periodiserteResultater.let {
                        val gammelG: Grunnbeloep = it[0].resultat.verdi
                        val forrigeGrunnbeloepDato = reguleringsmaaned.minusYears(1)
                        check(gammelG.dato == forrigeGrunnbeloepDato) {
                            "Dato til utledet forrige grunnbeløp er ikke forventet dato $forrigeGrunnbeloepDato"
                        }
                        val nyG: Grunnbeloep = it[1].resultat.verdi
                        check(nyG.dato == reguleringsmaaned) {
                            "Dato til utledet nytt grunnbeløp er ikke forventet dato $reguleringsmaaned"
                        }
                        Pair(gammelG, nyG)
                    }
                }
                is RegelkjoeringResultat.UgyldigPeriode ->
                    throw RuntimeException("Ugyldig regler for periode: ${resultat.ugyldigeReglerForPeriode}")
            }
        }
