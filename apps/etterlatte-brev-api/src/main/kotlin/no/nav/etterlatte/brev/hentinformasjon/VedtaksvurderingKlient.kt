package no.nav.etterlatte.brev.hentinformasjon

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.libs.ktorobo.ThrowableErrorMessage
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksvurderingKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    internal suspend fun hentVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto? {
        try {
            logger.info("Henter vedtaksvurdering behandling med behandlingId=$behandlingId")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/vedtak/$behandlingId"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (ex: ThrowableErrorMessage) {
            if (ex.cause is ResponseException) {
                haandterResponseException(ex.cause as ResponseException, behandlingId)
                return null
            } else {
                throw ex
            }
        }
    }

    private fun haandterResponseException(
        re: ResponseException,
        behandlingId: UUID,
    ) = if (er404(re)) {
        logger.info("Fant ikke vedtak for behandling $behandlingId. Dette er forventa hvis det f.eks. er et varselbrev.")
        null
    } else {
        throw ForespoerselException(
            status = re.response.status.value,
            code = "UKJENT_FEIL_HENTING_AV_VEDTAKSVURDERING",
            detail = "Ukjent feil oppsto ved henting av vedtak for behandling",
            meta = mapOf("behandlingId" to behandlingId),
        )
    }

    private fun er404(e: ResponseException) = e.response.status == HttpStatusCode.NotFound
}
