package no.nav.etterlatte.brev.vedtak

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

class VedtakvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class VedtaksvurderingKlient(config: Config, httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    suspend fun hentVedtak(behandlingId: String, accessToken: String): Vedtak {
        try {
            logger.info("Henter vedtaksvurdering behandling med behandlingId=$behandlingId")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/behandlinger/$behandlingId/fellesvedtak"),
                accessToken
            ).mapBoth(
                success = { resource -> resource.response.let { deserialize(it.toString()) } },
                failure = { errorResponse -> throw errorResponse }
            )
        } catch (e: Exception) {
            throw VedtakvurderingKlientException(
                "Henting vedtak for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }
}