package no.nav.etterlatte.brev.grunnlag

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

class GrunnlagKlient(config: Config, httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val baseUrl = config.getString("grunnlag.resource.url")

    suspend fun hentGrunnlag(sakid: Long, accessToken: String): Grunnlag {
        try {
            val json =
                downstreamResourceClient.get(
                    Resource(clientId, "$baseUrl/api/grunnlag/$sakid"),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { exception -> throw exception.throwable }
                ).response

            return deserialize(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av grunnlag for sakId:$sakid feilet")
            throw e
        }
    }
}