package no.nav.etterlatte.brev.beregning

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.UUID

class BeregningKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class BeregningKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(BeregningKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("beregning.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    suspend fun hentBeregning(behandlingId: UUID, accessToken: String): BeregningDTO {
        try {
            logger.info("Henter beregning (behandlingId: $behandlingId)")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/beregning/$behandlingId"),
                accessToken
            ).mapBoth(
                success = { resource -> deserialize(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse }
            )
        } catch (e: Exception) {
            throw BeregningKlientException(
                "Henting av beregning for behandling med behandlingId=$behandlingId feilet",
                e
            )
        }
    }
}