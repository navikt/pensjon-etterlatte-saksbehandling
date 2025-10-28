package no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt

import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

class PensjonsgivendeInntektService(
    private val sigrunKlient: SigrunKlient,
) {
    suspend fun hentSummerteInntekter(
        personident: String,
        aar: Int,
    ): SummertePensjonsgivendeInntekter {
        val pensjonsgivendeInntekt = sigrunKlient.hentPensjonsgivendeInntekt(personident, aar)

        val summertInntektFraSkatt = PensjonsgivendeInntektBeregning.beregnInntekt(pensjonsgivendeInntekt, aar)

        return SummertePensjonsgivendeInntekter(
            loensinntekt = summertInntektFraSkatt.verdi.loensinntekt,
            naeringsinntekt = summertInntektFraSkatt.verdi.naeringsinntekt,
            tidspunktBeregnet = Tidspunkt(summertInntektFraSkatt.opprettet),
            regelresultat = objectMapper.valueToTree(summertInntektFraSkatt),
        )
    }
}
