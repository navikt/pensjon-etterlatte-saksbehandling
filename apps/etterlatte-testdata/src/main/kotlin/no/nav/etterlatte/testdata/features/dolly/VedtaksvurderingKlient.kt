package no.nav.etterlatte.no.nav.etterlatte.testdata.features.dolly

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class VedtaksvurderingKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val vedtaksvurderingUrl = "${config.getString("vedtak.url")}/vedtak/fnr"

    suspend fun hentVedtak(request: Folkeregisteridentifikator): List<VedtakDto> {
        sikkerlogger().info("Henter vedtak med fnr=$request")

        return try {
            httpClient
                .post {
                    url(vedtaksvurderingUrl)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod feil i kall til vedtak API", e)
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw IkkeTillattException("VEDTAK-TILGANG", "Vedtak: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw UgyldigForespoerselException(
                    "VEDTAK-FORESPOERSEL",
                    "Vedtak: Ugyldig forespørsel",
                )

                HttpStatusCode.NotFound -> throw IkkeFunnetException(
                    "VEDTAK-IKKE-FUNNET",
                    "Vedtak: Ressurs ikke funnet",
                )

                else -> throw InternfeilException("Intern feil ved uthenting av vedtak")
            }
        }
    }
}

class VedtaksvurderingOboKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingOboKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client_id")
    private val resourceUrl = config.getString("vedtak.url")

    suspend fun hentVedtak(
        request: Folkeregisteridentifikator,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakDto> =
        try {
            downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/vedtak/fnr",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = request,
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod feil i kall til vedtak API", e)
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw IkkeTillattException("VEDTAK-TILGANG", "Vedtak: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw UgyldigForespoerselException(
                    "VEDTAK-FORESPOERSEL",
                    "Vedtak: Ugyldig forespørsel",
                )

                HttpStatusCode.NotFound -> throw IkkeFunnetException(
                    "VEDTAK-IKKE-FUNNET",
                    "Vedtak: Ressurs ikke funnet",
                )

                else -> throw InternfeilException("Intern feil ved uthenting av vedtak")
            }
        }
}
