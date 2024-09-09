package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.kombinerOverlappendePerioder
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.TrygdetidGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.anvendtTrygdetidRegel
import no.nav.etterlatte.beregning.regler.toSamlet
import no.nav.etterlatte.libs.common.beregning.SamletTrygdetidMedBeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.PeriodisertResultat
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import org.slf4j.LoggerFactory
import java.time.LocalDate

object BarnepensjonAnvendtTrygdetidPerioder {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun finnAnvendtTrygdetidPerioder(
        trygdetider: List<TrygdetidDto>,
        beregningsGrunnlag: BeregningsGrunnlag,
    ) = anvendtPerioder(beregningsGrunnlag.finnMuligeTrygdetidPerioder(trygdetider))

    fun finnKonstantTrygdetidPerioder(
        trygdetider: List<TrygdetidDto>,
        beregningsGrunnlag: BeregningsGrunnlag,
        fom: LocalDate,
    ): AnvendtTrygdetidPeriodeUtrekning {
        if (trygdetider.size != 1) {
            throw UgyldigForespoerselException(
                code = "FEIL_ANTALL_TRYGDETIDER",
                detail = "Fant flere trygdetider - men fikk bare ett beregningsgrunnlag",
            )
        }

        return trygdetider.first().toSamlet(beregningsGrunnlag.beregningsMetode.beregningsMetode)?.let {
            anvendtPerioder(listOf(GrunnlagMedPeriode(it, fom, null)))
        } ?: throw TrygdetidIkkeOpprettet()
    }

    private fun anvendtPerioder(muligePerioder: List<GrunnlagMedPeriode<SamletTrygdetidMedBeregningsMetode>>) =
        muligePerioder
            .map {
                anvendtTrygdetidRegel.eksekver(
                    KonstantGrunnlag(
                        TrygdetidGrunnlag(
                            FaktumNode(it.data, kilde = "Trygdetid", beskrivelse = "Beregnet trygdetid for avdød"),
                        ),
                    ),
                    RegelPeriode(it.fom, it.tom),
                )
            }.map {
                when (it) {
                    is RegelkjoeringResultat.Suksess -> it.periodiserteResultater.single()
                    is RegelkjoeringResultat.UgyldigPeriode -> throw InternfeilException(
                        "Ugyldig regler for periode: ${it.ugyldigeReglerForPeriode}",
                    )
                }
            }.map { aktueltResultat ->
                Pair(
                    GrunnlagMedPeriode(
                        data = aktueltResultat.resultat.verdi,
                        fom = aktueltResultat.periode.fraDato,
                        tom = aktueltResultat.periode.tilDato,
                    ),
                    aktueltResultat,
                )
            }.let {
                AnvendtTrygdetidPeriodeUtrekning(
                    anvendt = it.map { it.first }.kombinerOverlappendePerioder(),
                    utrekning = it.map { it.second },
                )
            }

    private fun BeregningsGrunnlag.finnMuligeTrygdetidPerioder(trygdetider: List<TrygdetidDto>) =
        beregningsMetodeFlereAvdoede.map { beregningsmetodeForAvdoedPeriode ->
            GrunnlagMedPeriode(
                data =
                    trygdetider
                        .finnForAvdoed(
                            beregningsmetodeForAvdoedPeriode.data.avdoed,
                        ).toSamlet(
                            beregningsmetodeForAvdoedPeriode.data.beregningsMetode.beregningsMetode,
                        ) ?: throw InternfeilException("Kunne ikke samle trygdetid for avdoed").also {
                        logger.warn("Kunne ikke samle trygdetid for avdoed - se sikkerlogg")
                        sikkerlogger().warn("Kunne ikke samle trygdetid for avdoed ${beregningsmetodeForAvdoedPeriode.data.avdoed}")
                    },
                fom = beregningsmetodeForAvdoedPeriode.fom,
                tom = beregningsmetodeForAvdoedPeriode.tom,
            )
        }

    fun List<TrygdetidDto>.finnForAvdoed(avdoed: String) =
        this.find { it.ident == avdoed }
            ?: throw InternfeilException("Manglende trygdetid for avdoed").also {
                logger.warn("Fant ikke trygdetid for avdoed - se sikkerlogg")
                sikkerlogger().warn("Fant ikke trygdetid for avdoed $avdoed i $this")
            }
}

data class AnvendtTrygdetidPeriodeUtrekning(
    val anvendt: List<GrunnlagMedPeriode<List<AnvendtTrygdetid>>>,
    val utrekning: List<PeriodisertResultat<AnvendtTrygdetid>>,
)
