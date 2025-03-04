package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService

class SigrunService(
    featureToggleService: FeatureToggleService,
) {
    fun hentPensjonsgivendeInntekt(ident: String): PensjonsgivendeInntektFraSkatt {
        // TODO klient mot Sigrun
        // val skalStubbe = featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_STUB_INNTEKT, false)
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
