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

class BeregningKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(BeregningKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("beregning.client.id")
    private val resourceUrl = config.getString("beregning.resource.url")

    suspend fun hentBeregning(behandlingId: String, accessToken: String): BeregningDTO {
        try {
            logger.info("Henter beregning (behandlingId: $behandlingId)")

            val json =
                downstreamResourceClient.get(
                    Resource(clientId, "$resourceUrl/api/beregning/$behandlingId"),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = {
                        logger.error("Henting av beregning for behandling (behandlingId=$behandlingId) feilet")
                        null
                    }
                )?.response

            return deserialize(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av beregning for behandling (behandlingId=$behandlingId) feilet")
            throw e
        }
    }
}