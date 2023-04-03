package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import org.slf4j.LoggerFactory
import java.time.LocalDate

object TrygdetidBeregningService {

    private val logger = LoggerFactory.getLogger(TrygdetidBeregningService::class.java)

    fun beregnTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>): BeregnetTrygdetid {
        logger.info("Beregner antall år trygdetid")

        val grunnlag = TotalTrygdetidGrunnlag(
            FaktumNode(
                verdi = trygdetidGrunnlag.mapNotNull { it.beregnetTrygdetid?.verdi },
                kilde = "System",
                beskrivelse = "Delberegninger all trygdetid"
            )
        )

        val resultat = beregnAntallAarTrygdetid.eksekver(grunnlag, RegelPeriode(LocalDate.now()))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val periodisertResultat = resultat.periodiserteResultater.first().resultat
                val totaltAntallAarTrygdetid = periodisertResultat.verdi

                logger.info("Beregning fullførte med resultat: $totaltAntallAarTrygdetid")
                BeregnetTrygdetid(
                    verdi = totaltAntallAarTrygdetid,
                    tidspunkt = Tidspunkt(periodisertResultat.opprettet),
                    regelResultat = periodisertResultat.toJsonNode()
                )
            }
            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }

    fun beregnTrygdetidGrunnlag(trygdetidGrunnlag: TrygdetidGrunnlag): TrygdetidGrunnlag {
        logger.info("Beregner trygdetid for trygdetidsgrunnlag ${trygdetidGrunnlag.id}")

        val grunnlag = TrygdetidPeriodeGrunnlag(
            periodeFra = FaktumNode(
                verdi = trygdetidGrunnlag.periode.fra,
                kilde = trygdetidGrunnlag.kilde,
                beskrivelse = "Startdato for perioden"
            ),
            periodeTil = FaktumNode(
                verdi = trygdetidGrunnlag.periode.til,
                kilde = trygdetidGrunnlag.kilde,
                beskrivelse = "Sluttdato for perioden"
            )
        )

        val resultat = beregnTrygdetidMellomToDatoer.eksekver(grunnlag, RegelPeriode(LocalDate.now()))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                val periodisertResultat = resultat.periodiserteResultater.first().resultat
                val periodeTrygdetid = periodisertResultat.verdi

                logger.info("Beregning fullførte med resultat: $periodeTrygdetid")

                trygdetidGrunnlag.copy(
                    beregnetTrygdetid = BeregnetTrygdetidGrunnlag(
                        verdi = periodeTrygdetid,
                        tidspunkt = Tidspunkt(periodisertResultat.opprettet),
                        regelResultat = periodisertResultat.toJsonNode()
                    )
                )
            }
            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }
}