package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.trygdetid.regler.TotalTrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.regler.TrygdetidPeriodeGrunnlag
import no.nav.etterlatte.trygdetid.regler.beregnAntallAarTotalTrygdetid
import no.nav.etterlatte.trygdetid.regler.beregnTrygdetidForPeriode
import no.nav.etterlatte.trygdetid.regler.totalTrygdetidYrkesskade
import org.slf4j.LoggerFactory
import java.time.LocalDate

object TrygdetidBeregningService {

    private val logger = LoggerFactory.getLogger(TrygdetidBeregningService::class.java)

    fun beregnTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>): BeregnetTrygdetid? {
        logger.info("Beregner antall år trygdetid")

        val beregnetTrygdetidListe = trygdetidGrunnlag.mapNotNull { it.beregnetTrygdetid }

        if (beregnetTrygdetidListe.isEmpty()) {
            logger.info("Har ingen perioder med beregnet trygdetidsgrunnlag.")
            return null
        }

        val grunnlag = TotalTrygdetidGrunnlag(
            FaktumNode(
                verdi = beregnetTrygdetidListe.map { it.verdi },
                kilde = "System",
                beskrivelse = "Beregninger alle trygdetidgrunnlag"
            )
        )

        val resultat = beregnAntallAarTotalTrygdetid.eksekver(KonstantGrunnlag(grunnlag), RegelPeriode(LocalDate.now()))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val periodisertResultat = resultat.periodiserteResultater.first().resultat
                val totaltAntallAarTrygdetid = periodisertResultat.verdi

                logger.info("Beregning fullførte med resultat: $totaltAntallAarTrygdetid år")
                BeregnetTrygdetid(
                    verdi = totaltAntallAarTrygdetid,
                    tidspunkt = Tidspunkt(periodisertResultat.opprettet),
                    regelResultat = periodisertResultat.toJsonNode()
                )
            }
            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }

    fun beregnTrygdetidForYrkesskade(kilde: Grunnlagsopplysning.Saksbehandler): BeregnetTrygdetid {
        logger.info("Beregner yrkkesskade trygdetid")

        val grunnlag = TotalTrygdetidGrunnlag(
            FaktumNode(
                verdi = emptyList(),
                kilde = kilde,
                beskrivelse = "Ingen grunnlag for yrkesskade"
            )
        )

        val resultat = totalTrygdetidYrkesskade.eksekver(KonstantGrunnlag(grunnlag), RegelPeriode(LocalDate.now()))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val periodisertResultat = resultat.periodiserteResultater.first().resultat
                val totaltAntallAarTrygdetid = periodisertResultat.verdi

                logger.info("Beregning fullførte med resultat: $totaltAntallAarTrygdetid år")
                BeregnetTrygdetid(
                    verdi = totaltAntallAarTrygdetid,
                    tidspunkt = Tidspunkt(periodisertResultat.opprettet),
                    regelResultat = periodisertResultat.toJsonNode()
                )
            }
            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }

    fun beregnTrygdetidGrunnlag(trygdetidGrunnlag: TrygdetidGrunnlag): BeregnetTrygdetidGrunnlag? {
        logger.info("Beregner trygdetid for trygdetidsgrunnlag ${trygdetidGrunnlag.id}")

        if (!trygdetidGrunnlag.erNasjonal()) {
            logger.info(
                "Kan ikke beregne trygdetidsgrunnlag for perioder utenfor Norge. Lagt inn ${trygdetidGrunnlag.bosted}"
            )
            return null
        }

        val grunnlag = TrygdetidPeriodeGrunnlag(
            periodeFra = FaktumNode(
                verdi = trygdetidGrunnlag.periode.fra,
                kilde = trygdetidGrunnlag.kilde,
                beskrivelse = "Startdato for trygdetidsperiode"
            ),
            periodeTil = FaktumNode(
                verdi = trygdetidGrunnlag.periode.til,
                kilde = trygdetidGrunnlag.kilde,
                beskrivelse = "Sluttdato for trygdetidsperiode"
            )
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
                    regelResultat = periodisertResultat.toJsonNode()
                )
            }
            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }
}