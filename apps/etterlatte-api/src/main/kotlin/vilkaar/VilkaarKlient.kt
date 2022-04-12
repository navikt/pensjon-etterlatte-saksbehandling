package no.nav.etterlatte.vilkaar

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.model.VurdertVilkaar
import org.slf4j.LoggerFactory

class VilkaarKlient(config: Config, httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(VilkaarKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    suspend fun vurdertVilkaar(behandlingId: String, accessToken: String): VurdertVilkaar {
        try {
            logger.info("Henter vurdert vilkaar for behandling $behandlingId")
            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/vurdertvilkaar/$behandlingId"
                    ), accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), VurdertVilkaar::class.java)
        } catch (e: Exception) {
            logger.error("Henting av person fra behandling feilet", e)
            throw e
        }
    }

}