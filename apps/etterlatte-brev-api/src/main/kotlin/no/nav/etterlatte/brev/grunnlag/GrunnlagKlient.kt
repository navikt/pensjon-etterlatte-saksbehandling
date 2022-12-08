package no.nav.etterlatte.brev.grunnlag

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

class GrunnlagKlient(config: Config, httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    suspend fun hentGrunnlag(sakid: Long, accessToken: String): Grunnlag {
        try {
            val json =
                downstreamResourceClient.get(
                    Resource(clientId, "$resourceUrl/api/grunnlag/$sakid"),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { errorMessage ->
                        logger.error("Henting av grunnlag for sakId:$sakid feilet", errorMessage.throwable)
                        null
                    }
                )?.response

            return json?.let { objectMapper.readValue(json.toString()) }!!
        } catch (e: Exception) {
            logger.error("Henting av grunnlag for sakId:$sakid feilet", e)
            throw e
        }
    }

    suspend fun hentGrunnlag(sakid: Long, type: Opplysningstype, accessToken: String): Grunnlagsopplysning<InnsenderSoeknad> {
        try {
            val json =
                downstreamResourceClient.get(
                    Resource(clientId, "$resourceUrl/api/grunnlag/$sakid/$type"),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { errorMessage ->
                        logger.error("Henting av grunnlag for sakId:$sakid feilet", errorMessage.throwable)
                        null
                    }
                )?.response

            return json?.let { objectMapper.readValue(json.toString()) }!!
        } catch (e: Exception) {
            logger.error("Henting av grunnlag for sakId:$sakid feilet", e)
            throw e
        }
    }
}
