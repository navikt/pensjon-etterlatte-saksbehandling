package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.trygdetid.regler.TrygdetidGrunnlagMedAvdoed
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodeGrunnlag
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodeMedPoengaar
import no.nav.etterlatte.trygdetid.regler.beregnDetaljertBeregnetTrygdetidMedYrkesskade
import no.nav.etterlatte.trygdetid.regler.beregnTrygdetidForPeriode
import org.slf4j.LoggerFactory
import java.time.LocalDate

object TrygdetidBeregningService {
    private val logger = LoggerFactory.getLogger(TrygdetidBeregningService::class.java)

    fun beregnTrygdetid(
        trygdetidGrunnlag: List<TrygdetidGrunnlag>,
        foedselsDato: LocalDate,
        doedsDato: LocalDate,
        norskPoengaar: Int?,
        yrkesskade: Boolean,
        nordiskKonvensjon: Boolean,
    ): DetaljertBeregnetTrygdetid? {
        logger.info("Beregner antall år trygdetid")

        val beregnetTrygdetidListe = trygdetidGrunnlag.mapNotNull { it.beregnetTrygdetid }

        if (beregnetTrygdetidListe.isEmpty() && norskPoengaar == null) {
            logger.info("Har ingen perioder med beregnet trygdetidsgrunnlag og ingen overstyrt poengår.")
            return null
        }

        val grunnlag =
            TrygdetidGrunnlagMedAvdoed(
                trygdetidGrunnlagListe = trygdetidGrunnlag,
                foedselsDato = foedselsDato,
                doedsDato = doedsDato,
                norskPoengaar = norskPoengaar,
                yrkesskade = yrkesskade,
                nordiskKonvensjon = nordiskKonvensjon,
            ).tilTrygdetidGrunnlagMedAvdoedGrunnlag(
                kilde = "System",
                beskrivelse = "Grunnlag for beregning av trygdetid",
            )

        val resultat =
            beregnDetaljertBeregnetTrygdetidMedYrkesskade.eksekver(
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

    fun beregnTrygdetidGrunnlag(trygdetidGrunnlag: TrygdetidGrunnlag): BeregnetTrygdetidGrunnlag? {
        logger.info("Beregner trygdetid for trygdetidsgrunnlag ${trygdetidGrunnlag.id}")

        val grunnlag =
            TrygdetidPeriodeGrunnlag(
                periode =
                    FaktumNode(
                        verdi =
                            TrygdetidPeriodeMedPoengaar(
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
