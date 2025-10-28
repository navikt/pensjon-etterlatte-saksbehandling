package no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektSummert
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektAarResponse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.SubsumsjonsNode
import no.nav.etterlatte.libs.regler.eksekver
import java.time.YearMonth

object PensjonsgivendeInntektBeregning {
    fun beregnInntekt(
        pensjonsgivendeInntekt: PensjonsgivendeInntektAarResponse,
        etteroppgjoersAar: Int,
    ): SubsumsjonsNode<PensjonsgivendeInntektSummert> {
        val grunnlag =
            KonstantGrunnlag(
                PensjonsgivendeInntektGrunnlag(
                    pensjonsgivendeInntektAarResponse =
                        FaktumNode(
                            verdi = pensjonsgivendeInntekt,
                            kilde = "Skatteetaten",
                            beskrivelse = "Inntekt for $etteroppgjoersAar fra Skatteetaten",
                        ),
                    aar =
                        FaktumNode(
                            verdi = etteroppgjoersAar,
                            kilde = "Etteroppgjørsår",
                            beskrivelse = "Året vi henter etteroppgjørsdata for",
                        ),
                ),
            )

        val resultat: RegelkjoeringResultat<PensjonsgivendeInntektSummert> =
            summerPensjonsgivendeInntekter.eksekver(
                grunnlag = grunnlag,
                periode =
                    RegelPeriode(
                        fraDato = YearMonth.of(etteroppgjoersAar, 1).atDay(1),
                        tilDato = null,
                    ),
            )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess ->
                resultat.periodiserteResultater.singleOrNull()?.resultat
                    ?: throw InternfeilException(
                        "Fikk 0 eller flere perioder tilbake i summering av inntekter fra" +
                            " Skatteetaten, som ikke skal være mulig",
                    )
            else -> throw InternfeilException(
                "Fikk ugyldig periode når inntekter fra Skatteetaten" +
                    " skulle beregnes for år $etteroppgjoersAar",
            )
        }
    }
}

data class SummertePensjonsgivendeInntekter(
    val loensinntekt: Int,
    val naeringsinntekt: Int,
    val tidspunktBeregnet: Tidspunkt?,
    val regelresultat: JsonNode? = null,
) {
    val summertInntekt = loensinntekt + naeringsinntekt
}
