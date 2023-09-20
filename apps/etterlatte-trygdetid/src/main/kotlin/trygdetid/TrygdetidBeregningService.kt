package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.trygdetid.regler.TotalTrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.regler.TrygdetidGrunnlagMedAvdoed
import no.nav.etterlatte.trygdetid.regler.TrygdetidGrunnlagMedAvdoedGrunnlag
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodMedPoengAar
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodeGrunnlag
import no.nav.etterlatte.trygdetid.regler.beregnDetaljertBeregnetTrygdetid
import no.nav.etterlatte.trygdetid.regler.beregnTrygdetidForPeriode
import no.nav.etterlatte.trygdetid.regler.totalTrygdetidYrkesskade
import org.slf4j.LoggerFactory
import java.time.LocalDate

object TrygdetidBeregningService {
    private val logger = LoggerFactory.getLogger(TrygdetidBeregningService::class.java)

    fun beregnTrygdetid(
        trygdetidGrunnlag: List<TrygdetidGrunnlag>,
        foedselsDato: LocalDate,
        doedsDato: LocalDate,
    ): DetaljertBeregnetTrygdetid? {
        logger.info("Beregner antall år trygdetid")

        val beregnetTrygdetidListe = trygdetidGrunnlag.mapNotNull { it.beregnetTrygdetid }

        if (beregnetTrygdetidListe.isEmpty()) {
            logger.info("Har ingen perioder med beregnet trygdetidsgrunnlag.")
            return null
        }

        val grunnlag =
            TrygdetidGrunnlagMedAvdoedGrunnlag(
                FaktumNode(
                    verdi =
                        TrygdetidGrunnlagMedAvdoed(
                            trygdetidGrunnlagListe = trygdetidGrunnlag,
                            foedselsDato = foedselsDato,
                            doedsDato = doedsDato,
                        ),
                    kilde = "System",
                    beskrivelse = "Beregn detaljert trygdetidsgrunnlag",
                ),
            )

        val resultat =
            beregnDetaljertBeregnetTrygdetid.eksekver(
                KonstantGrunnlag(grunnlag),
                RegelPeriode(LocalDate.now()),
            )
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val periodisertResultat = resultat.periodiserteResultater.first().resultat
                val detaljertTrygdetidVerdi = periodisertResultat.verdi

                logger.info("Beregning fullførte med resultat: $detaljertTrygdetidVerdi")
                DetaljertBeregnetTrygdetid(
                    resultat = detaljertTrygdetidVerdi,
                    tidspunkt = Tidspunkt(periodisertResultat.opprettet),
                    regelResultat = periodisertResultat.toJsonNode(),
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }

    fun beregnTrygdetidForYrkesskade(kilde: Grunnlagsopplysning.Saksbehandler): DetaljertBeregnetTrygdetid {
        logger.info("Beregner yrkkesskade trygdetid")

        val grunnlag =
            TotalTrygdetidGrunnlag(
                FaktumNode(
                    verdi = emptyList(),
                    kilde = kilde,
                    beskrivelse = "Ingen grunnlag for yrkesskade",
                ),
            )

        val resultat = totalTrygdetidYrkesskade.eksekver(KonstantGrunnlag(grunnlag), RegelPeriode(LocalDate.now()))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val periodisertResultat = resultat.periodiserteResultater.first().resultat
                val totaltAntallAarTrygdetid = periodisertResultat.verdi

                logger.info("Beregning fullførte med resultat: $totaltAntallAarTrygdetid år")
                DetaljertBeregnetTrygdetid(
                    resultat = DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(totaltAntallAarTrygdetid),
                    tidspunkt = Tidspunkt(periodisertResultat.opprettet),
                    regelResultat = periodisertResultat.toJsonNode(),
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }

    fun beregnTrygdetidGrunnlag(trygdetidGrunnlag: TrygdetidGrunnlag): BeregnetTrygdetidGrunnlag? {
        logger.info("Beregner trygdetid for trygdetidsgrunnlag ${trygdetidGrunnlag.id}")

        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodMedPoengAar(
                                fra = trygdetidGrunnlag.periode.fra,
                                til = trygdetidGrunnlag.periode.til,
                                poengInnAar = trygdetidGrunnlag.poengInnAar,
                                poengUtAar = trygdetidGrunnlag.poengUtAar,
                            ),
                        kilde = trygdetidGrunnlag.kilde,
                        beskrivelse = "Periode (med poeng aar) for trygdetidsperiode",
                    ),
            )

        val resultat = beregnTrygdetidForPeriode.eksekver(KonstantGrunnlag(grunnlag), RegelPeriode(LocalDate.now()))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val periodisertResultat = resultat.periodiserteResultater.first().resultat
                val periodeTrygdetid = periodisertResultat.verdi

                logger.info("Beregning fullførte med resultat: $periodeTrygdetid")

                BeregnetTrygdetidGrunnlag(
                    verdi = periodeTrygdetid,
                    tidspunkt = Tidspunkt(periodisertResultat.opprettet),
                    regelResultat = periodisertResultat.toJsonNode(),
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }
}
