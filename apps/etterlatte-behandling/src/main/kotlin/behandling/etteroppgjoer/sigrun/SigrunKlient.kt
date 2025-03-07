package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory

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
    private val logger = LoggerFactory.getLogger(SigrunKlientImpl::class.java)
    private val sikkerlogg = sikkerlogger()

    override suspend fun hentPensjonsgivendeInntekt(
        ident: String,
        inntektsaar: Int,
    ): PensjonsgivendeInntektFraSkatt {
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_STUB_INNTEKT, false)) {
            return PensjonsgivendeInntektFraSkatt.stub()
        }

        return retry {
            httpClient.get("$url/api/v1/pensjonsgivendeinntektforfolketrygden") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(body)
                headers.append("Nav-Personident", ident)
                headers.append("inntektsaar", inntektsaar.toString())
                headers.append("rettighetspakker", "navUfoeretrygd") // TODO: bytte ut med egen for team etterlatte
            }
        }.let {
            when (it) {
                is RetryResult.Success -> {
                    it.content.body<PensjonsgivendeInntektAarResponse>().fromResponse()
                }

                is RetryResult.Failure -> {
                    logger.error("Kall mot Sigrun feilet")
                    sikkerlogg.error("Kall mot Sigrun feilet for $ident")
                    throw it.samlaExceptions()
                }
            }
        }
    }
}

data class PensjonsgivendeInntektAarResponse(
    val inntektsaar: String,
    val norskPersonidentifikator: String,
    val pensjonsgivendeInntekt: List<PensjonsgivendeInntektResponse>,
)

data class PensjonsgivendeInntektResponse(
    val skatteordning: String,
    val pensjonsgivendeInntektAvLoennsinntekt: String,
    val pensjonsgivendeInntektAvNaeringsinntekt: String,
    val pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage: String,
)

fun PensjonsgivendeInntektAarResponse.fromResponse() =
    PensjonsgivendeInntektFraSkatt(
        inntektsaar = inntektsaar.toInt(),
        inntekter =
            pensjonsgivendeInntekt.map {
                PensjonsgivendeInntekt(
                    skatteordning = it.skatteordning,
                    loensinntekt = it.pensjonsgivendeInntektAvLoennsinntekt.toInt(),
                    naeringsinntekt = it.pensjonsgivendeInntektAvNaeringsinntekt.toInt(),
                    fiskeFangstFamiliebarnehage = it.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage.toInt(),
                    inntektsaar = inntektsaar.toInt(),
                )
            },
    )
