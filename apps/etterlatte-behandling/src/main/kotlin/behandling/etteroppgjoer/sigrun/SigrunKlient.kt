package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.HendelseslisteFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.SkatteoppgjoerHendelser
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktor.navConsumerId
import org.slf4j.LoggerFactory

interface SigrunKlient {
    suspend fun hentPensjonsgivendeInntekt(
        ident: String,
        inntektsaar: Int,
    ): PensjonsgivendeInntektFraSkatt

    suspend fun hentHendelsesliste(
        antall: Int,
        sekvensnummerStart: Long,
        brukAktoerId: Boolean = false,
    ): HendelseslisteFraSkatt
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
                navConsumerId("etterlatte-behandling")
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
                    logger.error("Kall mot Sigrun for henting av PensjonsgivendeInntekt feilet. Se sikkerlogg")
                    sikkerlogg.error("Kall mot Sigrun for henting av PensjonsgivendeInntekt feilet for ident=$ident")
                    throw it.samlaExceptions()
                }
            }
        }
    }

    override suspend fun hentHendelsesliste(
        antall: Int,
        sekvensnummerStart: Long,
        brukAktoerId: Boolean,
    ): HendelseslisteFraSkatt {
        if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_STUB_HENDELSER, false)) {
            return HendelseslisteFraSkatt.stub(sekvensnummerStart, antall)
        }

        return retry {
            httpClient.get("$url/api/v1/pensjonsgivendeinntektforfolketrygden/hendelser") {
                url {
                    parameters.append("fraSekvensnummer", sekvensnummerStart.toString())
                    parameters.append("antall", antall.toString())
                }
                navConsumerId("etterlatte-behandling")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(body)

                headers.append("bruk-aktoerid", brukAktoerId.toString())
            }
        }.let {
            when (it) {
                is RetryResult.Success -> {
                    HendelseslisteFraSkatt(
                        hendelser = it.content.body<List<SkatteoppgjoerHendelser>>(),
                    )
                }

                is RetryResult.Failure -> {
                    logger.error("Kall mot Sigrun for henting av Hendelsesliste feilet")
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
