package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.retry

interface SigrunKlient {
    suspend fun hentPensjonsgivendeInntekt(
        ident: String,
        inntektsaar: Int,
    ): PensjonsgivendeInntektFraSkatt
}

class SigrunKlientImpl(
    val httpClient: HttpClient,
    val url: String,
    val featureToggleService: FeatureToggleService,
) : SigrunKlient {
    override suspend fun hentPensjonsgivendeInntekt(
        ident: String,
        inntektsaar: Int,
    ): PensjonsgivendeInntektFraSkatt {
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_STUB_INNTEKT, false)) {
            return PensjonsgivendeInntektFraSkatt.stub()
        }

        retry {
            httpClient.get("$url/rest/v2/inntekt") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(body)
                headers.append("Nav-Personident", ident)
                headers.append("inntektsaar", inntektsaar.toString())
                headers.append("rettighetspakker", "navUfoeretrygd") // TODO: bytte ut med egen for team etterlatte
            }
        }.let {
            /*
            when (it) {
                is RetryResult.Success -> {
                    // TODO: mapping til vårt objekt
                    it.content.body<AInntektReponsData>()
                }

                is RetryResult.Failure -> {
                    // TODO: logge feil
                    logger.error("Kall mot inntektskomponent feilet")
                    sikkerlogg.error("Kall mot inntektskomponent feilet for $personident")
                    throw it.samlaExceptions()
                }
            }
             */
        }

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

// TODO: opprette database for Sigrun
