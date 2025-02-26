package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt

class SigrunService {
    fun hentPensjonsgivendeInntekt(ident: String): PensjonsgivendeInntektFraSkatt {
        // TODO klient mot Sigrun
        return PensjonsgivendeInntektFraSkatt.stub()
    }
}

/*
TODO for klient
data class PensjonsgivendeInntektAar(
    val inntektsaar: String,
    val pensjonsgivendeInntekt: List<PensjonsgivendeInntekt>
)

data class PensjonsgivendeInntekt(
    val skatteordning: String,
    val pensjonsgivendeInntektAvLoennsinntekt: String,
    val pensjonsgivendeInntektAvNaeringsinntekt: String,
    val pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage: String,
)
*/
